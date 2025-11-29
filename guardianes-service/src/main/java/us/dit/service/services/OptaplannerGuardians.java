package us.dit.service.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;

import org.springframework.transaction.annotation.Transactional;

import us.dit.service.model.entities.*;
import us.dit.service.model.entities.Calendar;
import us.dit.service.model.entities.Schedule.ScheduleStatus;
import us.dit.service.model.entities.Doctor.DoctorStatus;
import us.dit.service.model.entities.primarykeys.CalendarPK;
import us.dit.service.model.entities.score.GuardianesConstraintConfiguration;

import us.dit.service.model.repositories.CalendarRepository;
import us.dit.service.model.repositories.ScheduleRepository;
import us.dit.service.model.repositories.DoctorRepository;

/**
 * 
 */
@Lazy
@Slf4j
@Service
@RequiredArgsConstructor
public class OptaplannerGuardians {

    private YearMonth ym;
    private static final Logger logger = LoggerFactory.getLogger(OptaplannerGuardians.class);

    private final CalendarRepository calendarRepository;
    private final ScheduleRepository scheduleRepository;
    private final DoctorRepository doctorRepository;

    /**
     * @Autowired
     *            public OptaplannerGuardians(CalendarRepository calendarRepository,
     *            ScheduleRepository scheduleRepository, DoctorRepository
     *            doctorRepository) {
     *            this.calendarRepository = calendarRepository;
     *            this.scheduleRepository = scheduleRepository;
     *            this.doctorRepository = doctorRepository;
     *            }
     */

    @Transactional(readOnly = true)
    public Schedule solveProblem(YearMonth ym) {
        this.ym = ym;

        System.out.println("Construyendo el problema para " + ym.toString() + "...");
        Schedule schedule = buildProblem();
        System.out.println("Problema construido. Total de turnos: " + schedule.getShiftList().size()
                + ", Total de asignaciones: " + schedule.getShiftAssignments().size());

        System.setProperty(
                "javax.xml.parsers.DocumentBuilderFactory",
                "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");

        SolverFactory<Schedule> factory = SolverFactory.createFromXmlResource("solver/guardianesSolverConfig.xml");
        Solver<Schedule> solver = factory.buildSolver();

        System.out.println("Resolviendo... (esto puede tardar)");
        Schedule best = solver.solve(schedule);
        System.out.println("¡Resolución completada! Resultado final: " + best.getScore());
        // Cambio el estado a PENDING_CONFIRMATION
        // Si la planificación no cambia sigue en estado being_generated
        // faltaría hacer una gestión de las excepciones
        best.setStatus(ScheduleStatus.PENDING_CONFIRMATION);
        // Guardo la planificación optima
        this.scheduleRepository.save(best);
        return best;
    }

    private Schedule buildProblem() {

        CalendarPK pk = new CalendarPK(ym.getMonthValue(), ym.getYear());
        logger.info("Buscando calendario con PK: " + pk);

        Optional<Calendar> calendar = this.calendarRepository.findById(pk);
        if (!calendar.isPresent()) {
            logger.error("No se encontró calendario para " + ym);
            throw new RuntimeException("Trying to generate a schedule for a non existing calendar");
        }

        if (this.scheduleRepository.findById(pk).isPresent()) {
            logger.error("Ya existe un calendario generado para " + ym);
            throw new RuntimeException("The schedule is already generated");
        }

        Calendar cal = calendar.get();
        logger.info("Calendario encontrado: " + cal.getMonth() + "/" + cal.getYear());

        Schedule sch = new Schedule();
        sch.setId(pk);
        sch.setCalendar(cal);
        sch.setStatus(ScheduleStatus.BEING_GENERATED);

        GuardianesConstraintConfiguration conf = new GuardianesConstraintConfiguration(0L);
        sch.setConstraintConfiguration(conf);

        SortedSet<DayConfiguration> days = cal.getDayConfigurations();
        if (days == null || days.isEmpty()) {
            throw new RuntimeException("El calendario " + pk + " no tiene DayConfigurations cargadas.");
        }
        logger.info("Cargados " + days.size() + " DayConfigurations desde el calendario.");

        List<Shift> shifts = new ArrayList<>();
        long shiftSeq = 1L;
        for (DayConfiguration dc : cal.getDayConfigurations()) {
            Integer numTardes = dc.getNumShifts() != null ? dc.getNumShifts() : 0;
            Integer numConsultas = dc.getNumConsultations() != null ? dc.getNumConsultations() : 0;

            // SHIFT
            for (int i = 0; i < numTardes; i++) {
                Shift tarde = new Shift();
                tarde.setId(shiftSeq++);
                tarde.setShiftType("TARDE");
                tarde.setDayConfiguration(dc);
                tarde.setRequiresSkill(false);
                tarde.setConsultation(false);
                shifts.add(tarde);
            }

            // CONSULTATION
            for (int i = 0; i < numConsultas; i++) {
                Shift consulta = new Shift();
                consulta.setId(shiftSeq++); // Asignamos ID
                consulta.setShiftType("CONSULTA");
                consulta.setDayConfiguration(dc);
                consulta.setRequiresSkill(false); // Asumimos 'false'
                consulta.setConsultation(true); // Marcamos como consulta
                shifts.add(consulta);
            }

            // GUARDIA
            if (Boolean.TRUE.equals(dc.getIsWorkingDay()) && dc.getDay() % 2 == 0) {
                Shift guardia = new Shift();
                guardia.setId(shiftSeq++);
                guardia.setShiftType("GUARDIA");
                guardia.setDayConfiguration(dc);
                guardia.setRequiresSkill(false);
                guardia.setConsultation(false);
                shifts.add(guardia);
            }
        }

        logger.info("Generados " + shifts.size() + " instancias de Shift.");

        List<ShiftAssignment> assignments = new ArrayList<>();
        long saSeq = 1L;
        for (Shift s : shifts) {
            ShiftAssignment sa = new ShiftAssignment(s);
            sa.setId(saSeq++);
            sa.setSchedule(sch);
            sa.setDoctor(null);
            assignments.add(sa);
        }

        List<Doctor> allDoctors = this.doctorRepository.findAll();

        List<Doctor> allAvailableDoctors = allDoctors.stream().filter(d -> d.getStatus() == DoctorStatus.AVAILABLE)
                .collect(Collectors.toList());

        List<Doctor> doctorsForPlanning = new ArrayList<>();
        for (Doctor doc : allAvailableDoctors) {

            ShiftConfiguration sc = doc.getShiftConfiguration();

            if (sc == null) {
                logger.warn("El doctor " + doc.getId() + " (" + doc.getEmail()
                        + ") no tiene ShiftConfiguration. Se saltará.");
                continue;
            }
            doctorsForPlanning.add(doc);
        }

        if (doctorsForPlanning.isEmpty()) {
            throw new RuntimeException("No se encontraron doctores disponibles con ShiftConfiguration.");
        }
        logger.info(doctorsForPlanning.size() + " doctores con ShiftConfig añadidos a la planificación.");

        sch.setShiftAssignments(assignments);
        sch.setDoctorList(doctorsForPlanning);
        sch.setShiftList(shifts);
        sch.setDayConfigurationList(new ArrayList<>(cal.getDayConfigurations()));
        // Se guarda el schedule en estado BEING_GENERATED
        this.scheduleRepository.save(sch);

        return sch;
    }

}
