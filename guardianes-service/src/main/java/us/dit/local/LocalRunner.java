package us.dit.local;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;

import us.dit.service.model.entities.Schedule;
import us.dit.service.model.entities.Schedule.ScheduleStatus;
import us.dit.service.model.entities.Calendar;
import us.dit.service.model.entities.primarykeys.CalendarPK;
import us.dit.service.model.entities.DayConfiguration;
import us.dit.service.model.entities.Doctor;
import us.dit.service.model.entities.Shift;
import us.dit.service.model.entities.ShiftAssignment;

import us.dit.service.model.entities.ShiftConfiguration;
import us.dit.service.model.entities.score.GuardianesConstraintConfiguration;

public class LocalRunner {

    public static void main(String[] args) {
        // ---- CREA SCHEDULE ----
        YearMonth ym = YearMonth.of(2025, 8); 
        Schedule schedule = buildProblem(ym);
        
        System.setProperty(
                "javax.xml.parsers.DocumentBuilderFactory",
                "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");

        // ---- LANZA SOLVER ----
        SolverFactory<Schedule> factory =
                SolverFactory.createFromXmlResource("solver/guardianesSolverConfig.xml");
        Solver<Schedule> solver = factory.buildSolver();

        Schedule best = solver.solve(schedule);

        // ---- MUESTRA RESULTADOS ---- 
        System.out.println("\n=== RESULTADO ===");
        System.out.println("Score: " + best.getScore());
        System.out.println("Asignaciones:");
        best.getShiftAssignments().stream()
                .sorted(Comparator.comparing(a -> a.getDayConfiguration().getDate()))
                .forEach(a -> System.out.printf(
                        "%s | %-8s | doctor=%s | consult=%s\n",
                        a.getDayConfiguration().getDate(),
                        a.getShift().getShiftType(),
                        a.getDoctor() == null ? "-" : a.getDoctor().getId(),
                        String.valueOf(a.isConsultation())
                ));

        // ---- MUESTRA RESUMEN POR DIA ----
        Map<LocalDate, List<ShiftAssignment>> byDate =
        	    best.getShiftAssignments().stream()
        	        .collect(Collectors.groupingBy(
        	            sa -> java.time.LocalDate.of(
        	                     sa.getDayConfiguration().getYear(),
        	                     sa.getDayConfiguration().getMonth(),
        	                     sa.getDayConfiguration().getDay()
        	                 ),
        	            LinkedHashMap::new,
        	            Collectors.toList()
        	        ));
        System.out.println("\n=== RESUMEN POR DÍA ===");
        byDate.forEach((date, list) -> {
            long assigned = list.stream().filter(sa -> sa.getDoctor() != null).count();
            System.out.printf("%s -> %d/%d turnos asignados\n", date, assigned, list.size());
        });
    }

	// ---- CONSTRUYE UN SCHEDULE ----
	private static Schedule buildProblem(YearMonth ym) {
	    final int TOTAL_TARDE = 66;      // TARDES EN EL MES
	    final int TOTAL_CONSULT = 21;    // CONSULTAS EN EL MES
	    final int NUM_DOCTORS = 22;       // NUMERO DE DOCTORES
	
	    // ---- PK + Calendar ----
	    CalendarPK pk = new CalendarPK(ym.getMonthValue(), ym.getYear());
	    Calendar cal = new Calendar(ym.getMonthValue(), ym.getYear());
	
	    // ---- Schedule ----
	    Schedule sch = new Schedule();
	    setScheduleId(sch, pk);
	    safeSetMonthYear(sch, ym.getMonthValue(), ym.getYear());
	    sch.setCalendar(cal);
	    sch.setStatus(ScheduleStatus.BEING_GENERATED);
	
	    // ---- Constraint configuration ----
	    GuardianesConstraintConfiguration conf = new GuardianesConstraintConfiguration(0L);
	    sch.setConstraintConfiguration(conf);
	
	    // ---- DayConfigurations ----
	    SortedSet<DayConfiguration> days = new TreeSet<>();
	    for (int d = 1; d <= ym.lengthOfMonth(); d++) {
	        LocalDate date = ym.atDay(d);
	        boolean working = date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY;
	
	        DayConfiguration dc = new DayConfiguration(d, working, 0, 0);
	        dc.setDate(date);
	        dc.setCalendar(cal);
	        days.add(dc);
	    }
	    cal.setDayConfigurations(days);
	
	    // ---- FILTRAMOS LABORALES ----
	    List<DayConfiguration> workingDays = days.stream()
	        .filter(dc -> Boolean.TRUE.equals(dc.getIsWorkingDay()))
	        .sorted(Comparator.comparingInt(DayConfiguration::getDay))
	        .collect(Collectors.toList());
	
	    // ---- REPARTE TARDES Y CONSULTAS ----
	    distributePerDay(workingDays, TOTAL_TARDE, true);   // set numShifts (TARDE) por día
	    distributePerDay(workingDays, TOTAL_CONSULT, false); // set numConsultations por día
	
	    // ---- GENERA LOS SHIFTS POR DIA ----
	    List<Shift> shifts = new ArrayList<>();
	    long shiftSeq = 1L;
	    for (DayConfiguration dc : days) {
	        // TARDE
	        for (int i = 0; i < (dc.getNumShifts() == null ? 0 : dc.getNumShifts()); i++) {
	            Shift tarde = new Shift();
	            forcePlanningId(tarde, shiftSeq++);
	            tarde.setShiftType("TARDE");
	            tarde.setDayConfiguration(dc);
	            tarde.setRequiresSkill(false);
	            tarde.setConsultation(false);
	            shifts.add(tarde);
	        }
	        // CONSULTA 
	        for (int i = 0; i < (dc.getNumConsultations() == null ? 0 : dc.getNumConsultations()); i++) {
	            Shift consulta = new Shift();
	            forcePlanningId(consulta, shiftSeq++);
	            consulta.setShiftType("CONSULTA");
	            consulta.setDayConfiguration(dc);
	            consulta.setRequiresSkill(false);
	            consulta.setConsultation(true);
	            shifts.add(consulta);
	        }
	        // GUARDIA 
	        if (Boolean.TRUE.equals(dc.getIsWorkingDay()) && dc.getDay() % 2 == 0) {
	            Shift guardia = new Shift();
	            forcePlanningId(guardia, shiftSeq++);
	            guardia.setShiftType("GUARDIA");
	            guardia.setDayConfiguration(dc);
	            guardia.setRequiresSkill(false);
	            guardia.setConsultation(false);
	            shifts.add(guardia);
	        }
	    }
	
	    // ---- ShiftAssignments ----
	    List<ShiftAssignment> assignments = new ArrayList<>();
	    long saSeq = 1L;
	    for (Shift s : shifts) {
	        ShiftAssignment sa = new ShiftAssignment(s);
	        forcePlanningId(sa, saSeq++);
	        sa.setSchedule(sch);
	        sa.setDoctor(null); 
	        assignments.add(sa);
	    }
	
	    // ---- DOCTORS ----
	    List<Doctor> doctors = new ArrayList<>();
	    long docSeq = 1L;
	
	    // ---- REPARTO ENTRE DOTCORES ----
	    int baseConsultPerDoc = TOTAL_CONSULT / NUM_DOCTORS;
	    int remainderConsult = TOTAL_CONSULT % NUM_DOCTORS;
	
	    for (int i = 1; i <= NUM_DOCTORS; i++) {
	        String firstName = "Doc" + i;
	        String lastNames = "Test" + i;
	        String email = "doc" + i + "@example.com";
	        LocalDate startDate = YearMonth.of(ym.getYear(), ym.getMonth())
	                .atDay(Math.min(i, ym.lengthOfMonth()));
	
	        TestDoctor d = new TestDoctor(firstName, lastNames, email, startDate);
	        forcePlanningId(d, docSeq++);
	
	        int expectedConsults = baseConsultPerDoc + (i <= remainderConsult ? 1 : 0);
	
	        boolean canDoGuards = (i % 2 == 0); // la mitad pueden hacer guardias
	        ShiftConfiguration sc = new ShiftConfiguration(
	                0,     // minShifts TARDE
	                10,    // maxShifts TARDE 
	                expectedConsults, // numConsultations esperadas por doctor
	                canDoGuards, // doesCycleShifts
	                false  // hasShiftsOnlyWhenCycleShifts
	        );
	        sc.setDoctor(d);
	        d.setShiftConfiguration(sc);
	
	        doctors.add(d);
	    	}

	    // ---- CIERRRA SCHEDULE ----
	    sch.setShiftAssignments(assignments);
	    sch.setDoctorList(doctors);
	    sch.setShiftList(shifts);
	    sch.setDayConfigurationList(new ArrayList<>(days));
	
	    return sch;
	}
	
	// ---- DISTRIBUYE ENTRE LSO DIAS ----
	private static void distributePerDay(List<DayConfiguration> workingDays, int total, boolean isTarde) {
	    int n = workingDays.size();
	    int base = (n == 0) ? 0 : total / n;
	    int rem  = (n == 0) ? 0 : total % n;
	
	    for (int i = 0; i < workingDays.size(); i++) {
	        int val = base + (i < rem ? 1 : 0);
	        DayConfiguration dc = workingDays.get(i);
	        if (isTarde) {
	            dc.setNumShifts(val);          
	        } else {
	            dc.setNumConsultations(val);  
	        }
	    }
	}

    private static void setScheduleId(Schedule sch, CalendarPK pk) {
        try {
            sch.setId(pk);
        } catch (NoSuchMethodError | Exception ignored) {
        }
    }
    
    // ---- COMPRUEBA LOS DOCTORES ---- 
    public static class TestDoctor extends Doctor {
        @javax.persistence.Transient
        private ShiftConfiguration shiftConfiguration;

        public TestDoctor(String firstName, String lastNames, String email, java.time.LocalDate startDate) {
            super(firstName, lastNames, email, startDate);
        }
        // getter/setter que el DRL espera
        public ShiftConfiguration getShiftConfiguration() { 
        	return shiftConfiguration; 
    	}
        public void setShiftConfiguration(ShiftConfiguration sc) { 
        	this.shiftConfiguration = sc; 
    	}
    }
    
    private static void safeSetMonthYear(Schedule sch, int month, int year) {
        try { 
        	sch.setMonth(month); 
        	} catch (Throwable ignored) {}
        try { 
        	sch.setYear(year);  
        	} catch (Throwable ignored) {}
    }
    
    private static void forcePlanningId(Object bean, long idValue) {
        try {
            bean.getClass().getMethod("setId", Long.class).invoke(bean, idValue);
        } catch (Throwable ignore) {
            try {
                bean.getClass().getMethod("setId", long.class).invoke(bean, idValue);
            } catch (Throwable ignore2) {
                Class<?> c = bean.getClass();
                while (c != null) {
                    try {
                        java.lang.reflect.Field f = c.getDeclaredField("id");
                        f.setAccessible(true);
                        f.set(bean, idValue);
                        return;
                    } catch (NoSuchFieldException e) {
                        c = c.getSuperclass();
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("No puedo asignar id por reflexión a " + bean, e);
                    }
                }
                throw new RuntimeException("No encontré campo 'id' en la jerarquía de " + bean.getClass());
            }
        }
    }
}
