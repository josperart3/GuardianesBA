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

import us.dit.service.model.repositories.ShiftRepository;
import us.dit.service.model.repositories.CalendarRepository;
import us.dit.service.model.repositories.ScheduleRepository;
import us.dit.service.model.repositories.DoctorRepository;
import javax.persistence.EntityManager;

/**
 * 
 */
@Lazy
@Slf4j
@Service
@RequiredArgsConstructor
public class OptaplannerGuardians {

    private final CalendarRepository calendarRepository;
    private final ScheduleRepository scheduleRepository;
    private final DoctorRepository doctorRepository;
    private final ShiftRepository shiftRepository;
    private final EntityManager entityManager;

    @Transactional(timeout = 900)
    public Schedule solveProblem(YearMonth ym) {
        log.info(">>> 1. INICIO OptaplannerGuardians.solveProblem para {}", ym);

        // 1. Construir y GUARDAR el problema inicial
        Schedule managedProblem = buildAndSaveInitialProblem(ym);
        
        log.info(">>> 2. Problema persistido en BBDD. Iniciando Solver...");

        // 2. Configurar Solver
        SolverFactory<Schedule> factory = SolverFactory.createFromXmlResource("solver/guardianesSolverConfig.xml");
        Solver<Schedule> solver = factory.buildSolver();

        // 3. Resolver
        Schedule bestSolutionClone = solver.solve(managedProblem);

        // --- CÓDIGO DE DIAGNÓSTICO ---
        org.optaplanner.core.api.score.ScoreManager<Schedule> scoreManager = org.optaplanner.core.api.score.ScoreManager.create(factory);
        log.info("--- EXPLICACIÓN DEL SCORE ---");
        log.info(scoreManager.explainScore(bestSolutionClone));
        // -----------------------------
        
        log.info(">>> 3. Resolución completada! Score: {}", bestSolutionClone.getScore());

        log.info("--- TURNOS SIN ASIGNAR ---");
        bestSolutionClone.getShiftAssignments().stream()
            .filter(sa -> sa.getDoctor() == null)
            .forEach(sa -> log.info("VACÍO: Día {} - Tipo {}", sa.getDayConfiguration().getDate(), sa.getShift().getShiftType()));

        // 4. ACTUALIZACIÓN Y SINCRONIZACIÓN
        managedProblem.setStatus(Schedule.ScheduleStatus.PENDING_CONFIRMATION);
        managedProblem.setScore(bestSolutionClone.getScore());

        // --- FIX 1: Inicializar estructura Legacy (ScheduleDay) si es necesario ---
        if (managedProblem.getDays() == null) {
            managedProblem.setDays(new TreeSet<>()); 
        }

        // Si la lista está vacía, generamos los objetos ScheduleDay vacíos
        if (managedProblem.getDays().isEmpty()) {
            if (managedProblem.getDayConfigurationList() != null) {
                for (DayConfiguration dc : managedProblem.getDayConfigurationList()) {
                    ScheduleDay sd = new ScheduleDay();
                    sd.setDay(dc.getDay());
                    sd.setMonth(managedProblem.getMonth());
                    sd.setYear(managedProblem.getYear());
                    sd.setIsWorkingDay(dc.getIsWorkingDay());
                    sd.setSchedule(managedProblem);
                    
                    // Inicializamos las listas internas con ArrayList (compatible con List)
                    sd.setCycle(new ArrayList<>());
                    sd.setShifts(new ArrayList<>());
                    sd.setConsultations(new ArrayList<>());
                    
                    managedProblem.getDays().add(sd);
                }
            }
        }

        Map<Long, ShiftAssignment> solvedAssignmentsMap = bestSolutionClone.getShiftAssignments().stream()
                .collect(Collectors.toMap(sa -> sa.getShift().getId(), sa -> sa));

        // --- FIX 2: Sincronización de datos ---
        for (ShiftAssignment managedAssignment : managedProblem.getShiftAssignments()) {
            ShiftAssignment solvedAssignment = solvedAssignmentsMap.get(managedAssignment.getShift().getId());
            
            // Verificamos que exista la solución y que TENGA MÉDICO asignado
            if (solvedAssignment != null && solvedAssignment.getDoctor() != null) {
                
                // A. Actualizamos la entidad OptaPlanner
                managedAssignment.setDoctor(solvedAssignment.getDoctor());
                
                // B. Actualizamos la entidad Legacy (ScheduleDay) para la Vista HTML
                int dayNum = managedAssignment.getShift().getDayConfiguration().getDay();
                
                ScheduleDay scheduleDay = managedProblem.getDays().stream()
                    .filter(d -> d.getDay() == dayNum)
                    .findFirst()
                    .orElse(null);

                if (scheduleDay != null) {
                    // Protección defensiva adicional por si las listas vienen null de BBDD
                    if (scheduleDay.getCycle() == null) scheduleDay.setCycle(new ArrayList<>());
                    if (scheduleDay.getShifts() == null) scheduleDay.setShifts(new ArrayList<>());
                    if (scheduleDay.getConsultations() == null) scheduleDay.setConsultations(new ArrayList<>());

                    Doctor doctor = solvedAssignment.getDoctor();
                    String type = managedAssignment.getShift().getShiftType();

                    // Clasificamos el médico en la lista correcta según el tipo de turno
                    switch (type) {
                        case "GUARDIA": 
                            scheduleDay.getCycle().add(doctor);
                            break;
                        case "CONSULTA":
                            scheduleDay.getConsultations().add(doctor);
                            break;
                        default: // "TARDE"
                            scheduleDay.getShifts().add(doctor);
                            break;
                    }
                }
            }
        }

        // 5. Guardar cambios finales
        Schedule savedSchedule = this.scheduleRepository.save(managedProblem);
        
        log.info(">>> 4. Planificación guardada correctamente en BBDD con estado PENDING_CONFIRMATION");
        
        return savedSchedule;
    }

    private Schedule buildAndSaveInitialProblem(YearMonth ym) {
        CalendarPK pk = new CalendarPK(ym.getMonthValue(), ym.getYear());

        // --- LIMPIEZA ROBUSTA ---
        log.info("Limpiando datos antiguos...");

        // 1. PRIMERO: Borrar las asignaciones (Tabla hija final)
        entityManager.createQuery("DELETE FROM ShiftAssignment sa WHERE sa.schedule.month = :m AND sa.schedule.year = :y")
            .setParameter("m", ym.getMonthValue())
            .setParameter("y", ym.getYear())
            .executeUpdate();

        // 2. SEGUNDO: Borrar los días (ScheduleDay)
        entityManager.createQuery("DELETE FROM ScheduleDay sd WHERE sd.month = :m AND sd.year = :y")
            .setParameter("m", ym.getMonthValue())
            .setParameter("y", ym.getYear())
            .executeUpdate();

        // 3. TERCERO: Borrar el Schedule padre
        entityManager.createQuery("DELETE FROM Schedule s WHERE s.month = :m AND s.year = :y")
            .setParameter("m", ym.getMonthValue())
            .setParameter("y", ym.getYear())
            .executeUpdate();
        
        // 4. IMPORTANTE: Sincronizar y limpiar caché
        scheduleRepository.flush();
        entityManager.clear();
        
        // -------------------------------

        Calendar cal = this.calendarRepository.findById(pk)
                .orElseThrow(() -> new RuntimeException("No se encontró calendario para " + ym));

        log.info("Construyendo estructura inicial del problema...");

        // 2. Guardar Schedule Padre (Cabecera)
        Schedule sch = new Schedule();
        sch.setId(pk);
        sch.setCalendar(cal);
        sch.setStatus(Schedule.ScheduleStatus.BEING_GENERATED);
        sch.setConstraintConfiguration(new us.dit.service.model.entities.score.GuardianesConstraintConfiguration(0L));
        
        sch = this.scheduleRepository.saveAndFlush(sch);

        // 3. Crear y GUARDAR Shifts (Turnos)
        List<Shift> shiftsToSave = new ArrayList<>();
        for (DayConfiguration dc : cal.getDayConfigurations()) {
            if (Boolean.TRUE.equals(dc.getIsWorkingDay())) {
                Integer numTardes = dc.getNumShifts() != null ? dc.getNumShifts() : 0;
                Integer numConsultas = dc.getNumConsultations() != null ? dc.getNumConsultations() : 0;

                for (int i = 0; i < numTardes; i++) shiftsToSave.add(createShift(dc, "TARDE", false, false));
                for (int i = 0; i < numConsultas; i++) shiftsToSave.add(createShift(dc, "CONSULTA", false, true));
            }
            
            if (Boolean.TRUE.equals(dc.getIsWorkingDay()) && dc.getDay() % 2 == 0) {
                shiftsToSave.add(createShift(dc, "GUARDIA", false, false));
            }
        }
        
        log.info("Guardando {} turnos (Shifts)...", shiftsToSave.size());
        
        List<Shift> managedShifts = this.shiftRepository.saveAll(shiftsToSave);
        this.shiftRepository.flush();
        
        if (sch.getShiftList() == null) sch.setShiftList(new ArrayList<>());
        sch.getShiftList().clear();
        sch.getShiftList().addAll(managedShifts);

        // 4. Crear Assignments usando los Shifts GESTIONADOS
        List<ShiftAssignment> assignments = new ArrayList<>();
        
        for (Shift s : managedShifts) {
            ShiftAssignment sa = new ShiftAssignment(s);
            sa.setSchedule(sch); 
            assignments.add(sa);
        }
        
        if (sch.getShiftAssignments() == null) {
            sch.setShiftAssignments(new ArrayList<>());
        }
        
        sch.getShiftAssignments().clear();
        sch.getShiftAssignments().addAll(assignments);

        // 5. Configurar resto de datos
        List<Doctor> doctors = this.doctorRepository.findAll().stream()
                .filter(d -> d.getStatus() == Doctor.DoctorStatus.AVAILABLE && d.getShiftConfiguration() != null)
                .collect(Collectors.toList());

        for(Doctor d : doctors) {
            int currentMax = d.getShiftConfiguration().getMaxShifts();
            if (currentMax > 0) {
                d.getShiftConfiguration().setMaxShifts(30); 
            }
        }
        
        if(sch.getDoctorList() == null) sch.setDoctorList(new ArrayList<>());
        sch.getDoctorList().clear();
        sch.getDoctorList().addAll(doctors);

        if(sch.getDayConfigurationList() == null) sch.setDayConfigurationList(new ArrayList<>());
        sch.getDayConfigurationList().clear();
        sch.getDayConfigurationList().addAll(cal.getDayConfigurations());

        // 6. Guardado final
        log.info("Guardando estructura completa (Assignments, Doctors, etc)...");
        return this.scheduleRepository.saveAndFlush(sch);
    }
    
    private Shift createShift(DayConfiguration dc, String type, boolean skill, boolean consultation) {
        Shift s = new Shift();
        s.setShiftType(type);
        s.setDayConfiguration(dc);
        s.setRequiresSkill(skill);
        s.setConsultation(consultation);
        return s;
    }
}