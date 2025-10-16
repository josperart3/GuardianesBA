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
/**
 * The class LocalRunner builds an in-memory planning problem that it is solved with OptaPlanner. This 
 * class builds a Schedule for a given month and year, loads the solver configuration from guardianesSolverConfig.xml
 * and shows the score with the assignments that OptaPlanner built.
 * 
 * @author josperart3
 */
public class LocalRunner {

    public static void main(String[] args) {
        // ---- BUILD SCHEDULE ----
		// Can be changed to try whatever month 
        YearMonth ym = YearMonth.of(2025, 8); 
        Schedule schedule = buildProblem(ym);
        
		// This property forces the internal Xerces implementation to avoid incompatibilities with Drools and OptaPlanner
        System.setProperty(
			"javax.xml.parsers.DocumentBuilderFactory",
			"com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");

        // ---- LOAD SOLVER ----
        SolverFactory<Schedule> factory = SolverFactory.createFromXmlResource("solver/guardianesSolverConfig.xml");
        
		// ---- BUILD SOLVER ----
		Solver<Schedule> solver = factory.buildSolver();

		// ---- SOLVE THE PROBLEM ----
        Schedule best = solver.solve(schedule);

        // ---- SHOW RESULTS ---- 
        System.out.println("\n=== RESULT ===");
        System.out.println("Score: " + best.getScore());

		// ---- SHOW ASSIGNMENTS ----
		// Prints assignmentes ordered by date, showing shift and the assigned doctor
        System.out.println("Assignments:");
        best.getShiftAssignments().stream()
			.sorted(Comparator.comparing(a -> a.getDayConfiguration().getDate()))
			.forEach(a -> System.out.printf(
				"%s | %-8s | doctor=%s | consult=%s\n",
				a.getDayConfiguration().getDate(),
				a.getShift().getShiftType(),
				a.getDoctor() == null ? "-" : a.getDoctor().getId(),
				String.valueOf(a.isConsultation())));

        // ---- SHOW DAILY SUMMARY ----
        Map<LocalDate, List<ShiftAssignment>> byDate = best.getShiftAssignments().stream()
			.collect(Collectors.groupingBy(
				sa -> java.time.LocalDate.of(
						sa.getDayConfiguration().getYear(),
						sa.getDayConfiguration().getMonth(),
						sa.getDayConfiguration().getDay()
					),
				LinkedHashMap::new,
				Collectors.toList()
		));

		// For each day, it shows how many shifts have a doctor assigned and the total shifts for that day
        System.out.println("\n=== DAILY SUMMARY ===");
        byDate.forEach((date, list) -> {
            long assigned = list.stream().filter(sa -> sa.getDoctor() != null).count();
            System.out.printf("%s -> %d/%d turnos asignados\n", date, assigned, list.size());
        });
    }

	/**
	 * 
	 * Builds a schedule for a given month and year. It generates a DayConfiguration for each working day, shifts intances per day, 
	 * ShiftAssignment initially unassigned for doctors and a list of test doctors with ShiftConfiguration. It also distributes monthly totals
	 * of shift type for each working day.
	 * 
	 */
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
		// Constraint configuration instance used by OptaPlanner/Drools.
	    GuardianesConstraintConfiguration conf = new GuardianesConstraintConfiguration(0L);
	    sch.setConstraintConfiguration(conf);
	
	    // ---- DayConfigurations ----
		// Generates a DayConfiguration for each working day
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
	
	    // ---- FILTER WORKING DAYS ----
	    List<DayConfiguration> workingDays = days.stream()
	        .filter(dc -> Boolean.TRUE.equals(dc.getIsWorkingDay()))
	        .sorted(Comparator.comparingInt(DayConfiguration::getDay))
	        .collect(Collectors.toList());
	
	    // ---- DISTRIBUTES SHIFTS AND CONSULTATIONS ----
	    distributePerDay(workingDays, TOTAL_TARDE, true);   // set numShifts (TARDE) por día
	    distributePerDay(workingDays, TOTAL_CONSULT, false); // set numConsultations por día
	
	    // ---- GENERATE SHIFTS PER DAY ----
	    List<Shift> shifts = new ArrayList<>();
	    long shiftSeq = 1L;
	    for (DayConfiguration dc : days) {
	        // SHIFT
	        for (int i = 0; i < (dc.getNumShifts() == null ? 0 : dc.getNumShifts()); i++) {
	            Shift tarde = new Shift();
	            forcePlanningId(tarde, shiftSeq++);
	            tarde.setShiftType("TARDE");
	            tarde.setDayConfiguration(dc);
	            tarde.setRequiresSkill(false);
	            tarde.setConsultation(false);
	            shifts.add(tarde);
	        }
	        // CONSULTATION 
	        for (int i = 0; i < (dc.getNumConsultations() == null ? 0 : dc.getNumConsultations()); i++) {
	            Shift consulta = new Shift();
	            forcePlanningId(consulta, shiftSeq++);
	            consulta.setShiftType("CONSULTA");
	            consulta.setDayConfiguration(dc);
	            consulta.setRequiresSkill(false);
	            consulta.setConsultation(true);
	            shifts.add(consulta);
	        }
	        // GUARD 
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
		// Creates one assignment per shift, initially unassigned with a doctor. The solver will decide
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
	
	    // ---- DISTRIBUTE AMONG DOCTORS ----
	    int baseConsultPerDoc = TOTAL_CONSULT / NUM_DOCTORS;
	    int remainderConsult = TOTAL_CONSULT % NUM_DOCTORS;
	
	    for (int i = 1; i <= NUM_DOCTORS; i++) {
	        String firstName = "Doc" + i;
	        String lastNames = "Test" + i;
	        String email = "doc" + i + "@example.com";
	        LocalDate startDate = YearMonth.of(ym.getYear(), ym.getMonth()).atDay(Math.min(i, ym.lengthOfMonth()));
	
	        TestDoctor d = new TestDoctor(firstName, lastNames, email, startDate);
	        forcePlanningId(d, docSeq++);
	
	        int expectedConsults = baseConsultPerDoc + (i <= remainderConsult ? 1 : 0);
	
			// Half can do guards
	        boolean canDoGuards = (i % 2 == 0); 
	        ShiftConfiguration sc = new ShiftConfiguration(
				2,     // minShifts 
				5,    // maxShifts  
				expectedConsults, // numConsultations 
				canDoGuards, // doesCycleShifts
				false  // hasShiftsOnlyWhenCycleShifts
	        );
	        sc.setDoctor(d);
	        d.setShiftConfiguration(sc);
	
	        doctors.add(d);
	    	}

	    // ---- CLOSE SCHEDULE ----
	    sch.setShiftAssignments(assignments);
	    sch.setDoctorList(doctors);
	    sch.setShiftList(shifts);
	    sch.setDayConfigurationList(new ArrayList<>(days));
	
	    return sch;
	}
	
	/**
	 * Distributes a total amount across working days. Uses even split and adds 1 to the first 'rem' days.
	 * 
	 * @param workingDays
	 * @param total
	 * @param isTarde
	 */
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

	/**
	 * Tries to assign the Schedule id using the composite CalendarPK.
	 * 
	 * @param sch
	 * @param pk
	 */
    private static void setScheduleId(Schedule sch, CalendarPK pk) {
        try {
            sch.setId(pk);
        } catch (NoSuchMethodError | Exception ignored) {
        }
    }
    
    /**
	 * Test doctor that exposes a transient ShiftConfiguration so DRL and constraint can read it during planning.
	 */
    public static class TestDoctor extends Doctor {
        @javax.persistence.Transient
        private ShiftConfiguration shiftConfiguration;

        public TestDoctor(String firstName, String lastNames, String email, java.time.LocalDate startDate) {
            super(firstName, lastNames, email, startDate);
        }
    }
    
	/**
	 * Sets month and year in Schedule if those setters exist in your model version. Swallows any exception to maintain backward compatibility.
	 * 
	 * @param sch
	 * @param month
	 * @param year
	 */
    private static void safeSetMonthYear(Schedule sch, int month, int year) {
        try { 
			sch.setMonth(month); 
        	} catch (Throwable ignored) {}
        try { 
        	sch.setYear(year);  
        	} catch (Throwable ignored) {}
    }
    
	/**
	 * Forces assignment of a (long) ID to a domain bean via reflection. This ensures OptaPlanner has stable identifiers even for POJOs
	 * that do not expose the usual setter, or when the entity comes from older model versions with different signatures.
	 * 
	 * @param bean
	 * @param idValue
	 */
    private static void forcePlanningId(Object bean, long idValue) {
        try {
            bean.getClass().getMethod("setId", Long.class).invoke(bean, idValue);
        } catch (Throwable ignore) {
            try {
                bean.getClass().getMethod("setId", long.class).invoke(bean, idValue);
            } catch (Throwable ignore2) {
				// No setter found: look for an "id" field up the hierarchy.
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
