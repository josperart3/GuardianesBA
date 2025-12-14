package us.dit.service.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
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
import us.dit.service.model.entities.Shift;
import us.dit.service.model.entities.ShiftAssignment;
import us.dit.service.model.entities.ShiftConfiguration;
import us.dit.service.model.entities.primarykeys.CalendarPK;
import us.dit.service.model.repositories.CalendarRepository;
import us.dit.service.model.repositories.DoctorRepository;
import us.dit.service.model.repositories.ScheduleRepository;
import us.dit.service.model.repositories.ShiftRepository;

public class OptaplannerGuardiansTest {
    
    private OptaplannerGuardians planner;
    private CalendarRepository calendarRepository;
    private ScheduleRepository scheduleRepository;
    private DoctorRepository doctorRepository;
    private ShiftRepository shiftRepository;
    private EntityManager entityManager;
    
    // Constantes del servicio
    private static final int SHIFTS_BASE_POR_LABORABLE = 2;
    
    @BeforeEach
    void setUp() {
        calendarRepository = mock(CalendarRepository.class);
        scheduleRepository = mock(ScheduleRepository.class);
        doctorRepository = mock(DoctorRepository.class);
        shiftRepository = mock(ShiftRepository.class);
        entityManager = mock(EntityManager.class);
        
        Query mockedQuery = mock(Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(mockedQuery);
        when(mockedQuery.setParameter(anyString(), any())).thenReturn(mockedQuery);
        when(mockedQuery.executeUpdate()).thenReturn(1);

        planner = new OptaplannerGuardians(
            calendarRepository, 
            scheduleRepository, 
            doctorRepository, 
            shiftRepository, 
            entityManager
        );
    }

    @Test
    void testElasticDemandGeneration_HighDemand() {
        System.out.println("\n--- [Test Alta Demanda (Elástico)] ---");
        
        YearMonth ym = YearMonth.of(2026, 1);
        int diasLaborables = (int) calculateWorkingDays(ym);
        int baseCapacity = diasLaborables * SHIFTS_BASE_POR_LABORABLE;
        
        // Demanda > Capacidad
        int minShiftsPerDoc = 5;
        int numDoctors = 10;
        int totalDemand = minShiftsPerDoc * numDoctors; // 50
        
        setupMockRepositories(ym, numDoctors, 2, minShiftsPerDoc);
        
        Schedule solution = planner.solveProblem(ym);
        
        long totalTardes = solution.getShiftAssignments().stream()
                .filter(sa -> "TARDE".equals(sa.getShift().getShiftType()))
                .count();

        System.out.println("Días laborables: " + diasLaborables);
        System.out.println("Capacidad Base: " + baseCapacity);
        System.out.println("Demanda Médicos: " + totalDemand);
        System.out.println("Turnos TARDE generados: " + totalTardes);
        
        assertEquals(totalDemand, totalTardes, "Debe haber crecido para cubrir la demanda.");
    }

    @Test
    void testElasticDemandGeneration_LowDemand() {
        System.out.println("\n--- [Test Baja Demanda (Base)] ---");
        
        YearMonth ym = YearMonth.of(2026, 1); 
        int diasLaborables = (int) calculateWorkingDays(ym);
        int baseCapacity = diasLaborables * SHIFTS_BASE_POR_LABORABLE; // 44
        
        // Demanda < Capacidad
        int minShiftsPerDoc = 1;
        int numDoctors = 10;
        
        setupMockRepositories(ym, numDoctors, 2, minShiftsPerDoc);
        
        Schedule solution = planner.solveProblem(ym);
        
        long totalTardes = solution.getShiftAssignments().stream()
                .filter(sa -> "TARDE".equals(sa.getShift().getShiftType()))
                .count();

        System.out.println("Demanda Médicos: " + (numDoctors * minShiftsPerDoc));
        System.out.println("Turnos TARDE generados: " + totalTardes);

        assertEquals(baseCapacity, totalTardes, "Se debe respetar la capacidad base del hospital.");
    }

    // --- Helpers de Configuración ---

    private void setupMockRepositories(YearMonth ym, int numDoctors, int consultationsPerDoc, int minShiftsPerDoc) {
        CalendarPK pk = new CalendarPK(ym.getMonthValue(), ym.getYear());
        Calendar calendar = buildTestCalendar(ym);
        
        when(calendarRepository.findById(pk)).thenReturn(Optional.of(calendar));
        
        // --- AQUÍ ESTÁ EL FIX: Simular generación de IDs ---
        
        // 1. Mock ShiftRepository: Asigna IDs a los Shifts
        when(shiftRepository.saveAll(any())).thenAnswer(i -> {
            List<Shift> list = (List<Shift>) i.getArguments()[0];
            long id = 1000;
            for (Shift s : list) {
                setEntityId(s, id++);
            }
            return list;
        });

        // 2. Mock ScheduleRepository: Asigna IDs al Schedule Y a los ShiftAssignments
        // Esto es crucial porque en tu código de producción se guardan en cascada/flush
        when(scheduleRepository.saveAndFlush(any(Schedule.class))).thenAnswer(i -> {
            Schedule s = (Schedule) i.getArguments()[0];
            if (s.getId() == null) {
                // El ID del Schedule es compuesto (CalendarPK), pero si necesitara uno long:
                // setEntityId(s, 1L); 
                // En este caso CalendarPK ya viene seteado en el código, no es Long.
            }

            // SIMULAMOS EL EFECTO CASCADA DE JPA
            // Asignamos IDs a los ShiftAssignments que contiene
            if (s.getShiftAssignments() != null) {
                long saId = 5000;
                for (ShiftAssignment sa : s.getShiftAssignments()) {
                    // Si no tiene ID, se lo ponemos
                    if (sa.getId() == null) {
                        setEntityId(sa, saId++);
                    }
                }
            }
            return s;
        });
        
        // 3. Doctores
        List<Doctor> doctors = buildTestDoctors(numDoctors, consultationsPerDoc, minShiftsPerDoc);
        when(doctorRepository.findAll()).thenReturn(doctors);
    }
    
    // Helper usando Reflexión para asignar IDs a campos privados/protegidos
    private void setEntityId(Object entity, Long id) {
        try {
            Class<?> clazz = entity.getClass();
            Field idField = null;
            
            // Buscar el campo 'id' en la clase o sus padres (AbstractPersistable)
            while (clazz != null) {
                try {
                    idField = clazz.getDeclaredField("id");
                    break;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            
            if (idField != null) {
                idField.setAccessible(true);
                idField.set(entity, id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private Calendar buildTestCalendar(YearMonth ym) {
        Calendar cal = new Calendar(ym.getMonthValue(), ym.getYear());
        SortedSet<DayConfiguration> days = new TreeSet<>(Comparator.comparing(DayConfiguration::getDay));
        
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            LocalDate date = ym.atDay(d);
            boolean isWorkingDay = date.getDayOfWeek() != DayOfWeek.SATURDAY 
                                && date.getDayOfWeek() != DayOfWeek.SUNDAY;
            DayConfiguration dc = new DayConfiguration(d, isWorkingDay, 0, 0);
            dc.setDate(date);
            dc.setCalendar(cal);
            
            // Asignar ID al DayConfiguration para evitar problemas
            setEntityId(dc, (long) d);
            
            days.add(dc);
        }
        cal.setDayConfigurations(days);
        return cal;
    }
    
    private List<Doctor> buildTestDoctors(int count, int consultationsPerDoc, int minShiftsPerDoc) {
        List<Doctor> doctors = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Doctor doc = new Doctor();
            doc.setFirstName("Doctor" + i);
            setEntityId(doc, (long) i); // Usamos el helper aquí también
            
            try {
                java.lang.reflect.Field statusField = Doctor.class.getDeclaredField("status");
                statusField.setAccessible(true);
                statusField.set(doc, DoctorStatus.AVAILABLE);
            } catch (Exception e) {}
            
            ShiftConfiguration sc = new ShiftConfiguration();
            sc.setDoctor(doc);
            sc.setDoctorId((long) i);
            sc.setMaxShifts(10);
            sc.setMinShifts(minShiftsPerDoc);
            sc.setNumConsultations(consultationsPerDoc);
            sc.setDoesCycleShifts(true); 
            
            doc.setShiftConfiguration(sc);
            doctors.add(doc);
        }
        return doctors;
    }
    
    private long calculateWorkingDays(YearMonth ym) {
        long workingDays = 0;
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            DayOfWeek dow = ym.atDay(d).getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                workingDays++;
            }
        }
        return workingDays;
    }
}