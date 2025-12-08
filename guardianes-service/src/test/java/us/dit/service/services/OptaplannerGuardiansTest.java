package us.dit.service.services;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import us.dit.service.model.entities.Calendar;
import us.dit.service.model.entities.DayConfiguration;
import us.dit.service.model.entities.Doctor;
import us.dit.service.model.entities.Doctor.DoctorStatus;
import us.dit.service.model.entities.Schedule;
import us.dit.service.model.entities.ShiftAssignment;
import us.dit.service.model.entities.ShiftConfiguration;
import us.dit.service.model.entities.primarykeys.CalendarPK;
import us.dit.service.model.repositories.CalendarRepository;
import us.dit.service.model.repositories.DoctorRepository;
import us.dit.service.model.repositories.ScheduleRepository;
import us.dit.service.model.repositories.ShiftRepository;

/**
 * Test unitario de OptaplannerGuardians usando Mockito.
 * Actualizado para soportar las dependencias de ShiftRepository y EntityManager.
 */
public class OptaplannerGuardiansTest {
    
    private OptaplannerGuardians planner;
    private CalendarRepository calendarRepository;
    private ScheduleRepository scheduleRepository;
    private DoctorRepository doctorRepository;
    private ShiftRepository shiftRepository;     // Nueva dependencia
    private EntityManager entityManager;         // Nueva dependencia
    
    @BeforeEach
    void setUp() {
        // 1. Crear mocks de todos los repositorios y el EntityManager
        calendarRepository = mock(CalendarRepository.class);
        scheduleRepository = mock(ScheduleRepository.class);
        doctorRepository = mock(DoctorRepository.class);
        shiftRepository = mock(ShiftRepository.class);
        entityManager = mock(EntityManager.class);
        
        // 2. Configurar el comportamiento básico del EntityManager para evitar NPE en las queries de limpieza
        Query mockedQuery = mock(Query.class);
        // Cuando se pida crear una query, devolver el mock
        when(entityManager.createQuery(anyString())).thenReturn(mockedQuery);
        // Permitir encadenamiento de setParameter: query.setParameter(...) devuelve query
        when(mockedQuery.setParameter(anyString(), any())).thenReturn(mockedQuery);
        // Cuando se ejecute update, devolver 1 (filas afectadas dummy)
        when(mockedQuery.executeUpdate()).thenReturn(1);

        // 3. Crear instancia del servicio inyectando TODOS los mocks en el orden declarado en la clase
        planner = new OptaplannerGuardians(
            calendarRepository, 
            scheduleRepository, 
            doctorRepository, 
            shiftRepository, 
            entityManager
        );
    }

    @Test
    void testOptaplannerGuardiansClass() {
        System.out.println("\n--- [Test de OptaplannerGuardians CON MOCKS] ---");
        
        YearMonth ym = YearMonth.of(2026, 1);
        
        // Configurar comportamiento de los mocks de datos
        setupMockRepositories(ym);
        
        // Ejecutar el método solveProblem
        System.out.println("Ejecutando planner.solveProblem(" + ym + ")...");
        
        // NOTA: Esto intentará cargar 'solver/guardianesSolverConfig.xml'. 
        // Si el test falla aquí, asegúrate de que esa carpeta está en src/main/resources
        Schedule solution = planner.solveProblem(ym);
        
        // Validar resultado
        assertNotNull(solution, "La solución no debe ser null");
        assertNotNull(solution.getScore(), "El score no debe ser null");
        
        long assigned = solution.getShiftAssignments().stream()
            .filter(sa -> sa.getDoctor() != null)
            .count();
        
        printResults(solution);
        
        assertTrue(assigned > 0, "Se esperaba que se asignara al menos un turno. Revisa la configuración del Solver o los datos mock.");
        
        System.out.println("--- [Test FINALIZADO EXITOSAMENTE] ---\n");
    }
    
    private void setupMockRepositories(YearMonth ym) {
        CalendarPK pk = new CalendarPK(ym.getMonthValue(), ym.getYear());
        
        // A. Mock Calendar
        Calendar calendar = buildTestCalendar(ym);
        when(calendarRepository.findById(pk)).thenReturn(Optional.of(calendar));
        
        // B. Mock Schedule (Simular comportamiento de guardado)
        // Cuando se llame a save o saveAndFlush, devolver el mismo objeto que se pasó (simular persistencia exitosa)
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(i -> i.getArguments()[0]);
        when(scheduleRepository.saveAndFlush(any(Schedule.class))).thenAnswer(i -> i.getArguments()[0]);
        
        // C. Mock Shift (Simular guardado de lista)
        when(shiftRepository.saveAll(any())).thenAnswer(i -> i.getArguments()[0]);
        
        // D. Mock Doctors
        List<Doctor> doctors = buildTestDoctors(22);
        when(doctorRepository.findAll()).thenReturn(doctors);
    }
    
    private Calendar buildTestCalendar(YearMonth ym) {
        Calendar cal = new Calendar(ym.getMonthValue(), ym.getYear());
        
        // CORRECCIÓN: Usar SortedSet (TreeSet) en lugar de List
        // Añadimos un comparador para que el TreeSet sepa ordenar los días (por número de día)
        SortedSet<DayConfiguration> days = new TreeSet<>(Comparator.comparing(DayConfiguration::getDay));
        
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            LocalDate date = ym.atDay(d);
            boolean isWorkingDay = date.getDayOfWeek() != DayOfWeek.SATURDAY 
                                && date.getDayOfWeek() != DayOfWeek.SUNDAY;
            
            int numShifts = isWorkingDay ? 3 : 0; 
            int numConsultations = isWorkingDay ? 1 : 0;
            
            DayConfiguration dc = new DayConfiguration(d, isWorkingDay, numShifts, numConsultations);
            dc.setDate(date);
            dc.setCalendar(cal);
            days.add(dc);
        }
        
        cal.setDayConfigurations(days); // Ahora sí coincide el tipo (SortedSet)
        return cal;
    }
    
    private List<Doctor> buildTestDoctors(int count) {
        List<Doctor> doctors = new ArrayList<>();
        
        for (int i = 1; i <= count; i++) {
            Doctor doc = new Doctor();
            doc.setFirstName("Doctor" + i);
            doc.setLastNames("Apellido" + i);
            doc.setEmail("doctor" + i + "@test.com");
            doc.setStartDate(LocalDate.now().minusYears(1));
            
            // Setear ID y Status usando reflection para evitar restricciones de JPA
            try {
                java.lang.reflect.Field idField = Doctor.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(doc, (long) i);
                
                java.lang.reflect.Field statusField = Doctor.class.getDeclaredField("status");
                statusField.setAccessible(true);
                statusField.set(doc, DoctorStatus.AVAILABLE);
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // Configuración de turnos para que OptaPlanner pueda asignarles trabajo
            ShiftConfiguration sc = new ShiftConfiguration();
            sc.setDoctorId((long) i);
            sc.setDoctor(doc);
            sc.setMaxShifts(20); // Damos capacidad suficiente
            sc.setMinShifts(0);
            sc.setNumConsultations(5);
            sc.setDoesCycleShifts(true);
            
            doc.setShiftConfiguration(sc);
            doctors.add(doc);
        }
        return doctors;
    }
    
    private void printResults(Schedule solution) {
        if (solution == null) return;

        System.out.println("\n=== RESULTADO FINAL ===");
        System.out.println("Score: " + solution.getScore());
        
        // Evitar NullPointerException en los logs si las listas son null
        List<ShiftAssignment> assignments = solution.getShiftAssignments() != null ? solution.getShiftAssignments() : Collections.emptyList();

        long assignedCount = assignments.stream().filter(sa -> sa.getDoctor() != null).count();
        
        System.out.println("Turnos totales: " + assignments.size());
        System.out.println("Asignados: " + assignedCount);
        
        // Mostrar muestra
        assignments.stream()
            .filter(sa -> sa.getDoctor() != null)
            .limit(5)
            .forEach(a -> System.out.println("Día " + a.getDayConfiguration().getDay() + ": " + a.getDoctor().getFirstName()));
    }
}