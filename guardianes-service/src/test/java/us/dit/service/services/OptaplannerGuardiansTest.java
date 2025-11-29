package us.dit.service.services;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

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

/**
 * Test unitario de OptaplannerGuardians usando Mockito.
 * Simula los repositorios JPA para probar la clase sin Spring Boot.
 */
public class OptaplannerGuardiansTest {
    
    private OptaplannerGuardians planner;
    private CalendarRepository calendarRepository;
    private ScheduleRepository scheduleRepository;
    private DoctorRepository doctorRepository;
    
    @BeforeEach
    void setUp() {
        // Crear mocks de los repositorios
        calendarRepository = mock(CalendarRepository.class);
        scheduleRepository = mock(ScheduleRepository.class);
        doctorRepository = mock(DoctorRepository.class);
        
        // Crear instancia de OptaplannerGuardians con mocks inyectados
        planner = new OptaplannerGuardians(calendarRepository, scheduleRepository, doctorRepository);
    }

    @Test
    void testOptaplannerGuardiansClass() {
        System.out.println("\n--- [Test de OptaplannerGuardians CON MOCKS] ---");
        
        YearMonth ym = YearMonth.of(2025, 8);
        
        // Configurar comportamiento de los mocks
        setupMockRepositories(ym);
        
        // Ejecutar el método solveProblem de OptaplannerGuardians
        System.out.println("Ejecutando planner.solveProblem(" + ym + ")...");
        Schedule solution = planner.solveProblem(ym);
        
        // Validar resultado
        assertNotNull(solution, "La solución no debe ser null");
        assertNotNull(solution.getScore(), "El score no debe ser null");
        
        long assigned = solution.getShiftAssignments().stream()
            .filter(sa -> sa.getDoctor() != null)
            .count();
        
        printResults(solution);
        
        assertTrue(assigned > 0, "Se esperaba que se asignara al menos un turno");
        
        System.out.println("--- [Test FINALIZADO EXITOSAMENTE] ---\n");
    }
    
    /**
     * Configura los mocks de repositorios con datos de prueba realistas
     */
    private void setupMockRepositories(YearMonth ym) {
        CalendarPK pk = new CalendarPK(ym.getMonthValue(), ym.getYear());
        
        // Mock CalendarRepository - devuelve calendario con días configurados
        Calendar calendar = buildTestCalendar(ym);
        when(calendarRepository.findById(pk)).thenReturn(Optional.of(calendar));
        
        // Mock ScheduleRepository - no existe schedule previo (primera generación)
        when(scheduleRepository.findById(any())).thenReturn(Optional.empty());
        
        // Mock DoctorRepository - devuelve lista de doctores disponibles
        List<Doctor> doctors = buildTestDoctors(22);
        when(doctorRepository.findAll()).thenReturn(doctors);
    }
    
    /**
     * Construye un Calendar de prueba con DayConfigurations realistas
     */
    private Calendar buildTestCalendar(YearMonth ym) {
        Calendar cal = new Calendar(ym.getMonthValue(), ym.getYear());
        
        SortedSet<DayConfiguration> days = new TreeSet<>();
        
        // Configurar días del mes
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            LocalDate date = ym.atDay(d);
            boolean isWorkingDay = date.getDayOfWeek() != DayOfWeek.SATURDAY 
                                && date.getDayOfWeek() != DayOfWeek.SUNDAY;
            
            // Solo días laborables tienen turnos
            int numShifts = isWorkingDay ? 3 : 0;  // 3 turnos TARDE por día laborable
            int numConsultations = isWorkingDay ? 1 : 0;  // 1 CONSULTA por día laborable
            
            DayConfiguration dc = new DayConfiguration(d, isWorkingDay, numShifts, numConsultations);
            dc.setDate(date);
            dc.setCalendar(cal);
            days.add(dc);
        }
        
        cal.setDayConfigurations(days);
        
        System.out.println("Calendar mock creado: " + ym + " con " + days.size() + " días");
        return cal;
    }
    
    /**
     * Construye lista de doctores de prueba con ShiftConfiguration
     */
    private List<Doctor> buildTestDoctors(int count) {
        List<Doctor> doctors = new ArrayList<>();
        
        for (int i = 1; i <= count; i++) {
            Doctor doc = new Doctor(
                "Doctor" + i,
                "Apellido" + i,
                "doctor" + i + "@test.com",
                LocalDate.now().minusYears(5)
            );
            
            // Setear ID usando reflection
            try {
                java.lang.reflect.Field idField = Doctor.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(doc, (long) i);
                
                // Setear status a AVAILABLE
                java.lang.reflect.Field statusField = Doctor.class.getDeclaredField("status");
                statusField.setAccessible(true);
                statusField.set(doc, DoctorStatus.AVAILABLE);
            } catch (Exception e) {
                System.err.println("No se pudo setear ID/status al doctor: " + e.getMessage());
            }
            
            // Crear ShiftConfiguration
            ShiftConfiguration sc = new ShiftConfiguration();
            sc.setDoctorId((long) i);
            sc.setDoctor(doc);
            sc.setMaxShifts(10);
            sc.setMinShifts(2);
            sc.setNumConsultations(1);
            sc.setDoesCycleShifts(true);
            sc.setHasShiftsOnlyWhenCycleShifts(false);
            
            // Asignar ShiftConfiguration al doctor
            try {
                java.lang.reflect.Field scField = Doctor.class.getDeclaredField("shiftConfiguration");
                scField.setAccessible(true);
                scField.set(doc, sc);
            } catch (Exception e) {
                System.err.println("No se pudo setear ShiftConfiguration: " + e.getMessage());
            }
            
            doctors.add(doc);
        }
        
        System.out.println("Doctores mock creados: " + doctors.size());
        return doctors;
    }
    
    /**
     * Imprime resultados de la solución
     */
    private void printResults(Schedule solution) {
        if (solution == null) {
            System.out.println("ERROR: Schedule es null");
            return;
        }

        System.out.println("\n=== RESULTADO FINAL ===");
        System.out.println("Score: " + solution.getScore());
        System.out.println("Mes/Año: " + solution.getMonth() + "/" + solution.getYear());

        long totalAssignments = solution.getShiftAssignments().size();
        long assignedCount = solution.getShiftAssignments().stream()
            .filter(sa -> sa.getDoctor() != null)
            .count();
        
        System.out.println("\n=== ESTADÍSTICAS ===");
        System.out.println("Total turnos: " + totalAssignments);
        System.out.println("Turnos asignados: " + assignedCount);
        System.out.println("Turnos sin asignar: " + (totalAssignments - assignedCount));

        // Muestra primeras 15 asignaciones
        System.out.println("\n=== PRIMERAS 15 ASIGNACIONES ===");
        solution.getShiftAssignments().stream()
            .sorted(Comparator.comparing(a -> a.getDayConfiguration().getDate()))
            .limit(15)
            .forEach(a -> {
                String doctorInfo = a.getDoctor() == null ? "SIN ASIGNAR" : "Doctor-" + a.getDoctor().getId();
                System.out.printf("%s | %-8s | %s\n",
                    a.getDayConfiguration().getDate(),
                    a.getShift().getShiftType(),
                    doctorInfo);
            });

        // Resumen por día
        Map<LocalDate, List<ShiftAssignment>> byDate = 
            solution.getShiftAssignments().stream()
                .collect(Collectors.groupingBy(
                    sa -> sa.getDayConfiguration().getDate(),
                    LinkedHashMap::new,
                    Collectors.toList()
                ));

        System.out.println("\n=== RESUMEN POR DÍA (primeros 10) ===");
        byDate.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .limit(10)
            .forEach(entry -> {
                long dayAssigned = entry.getValue().stream()
                    .filter(sa -> sa.getDoctor() != null)
                    .count();
                System.out.printf("%s -> %d/%d asignados\n",
                    entry.getKey(), dayAssigned, entry.getValue().size());
            });
    }
}
