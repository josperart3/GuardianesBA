/**
*  This file is part of GuardianesBA - Business Application for processes managing healthcare tasks planning and supervision.
*  Copyright (C) 2024  Universidad de Sevilla/Departamento de Ingeniería Telemática
*
*  GuardianesBA is free software: you can redistribute it and/or
*  modify it under the terms of the GNU General Public License as published
*  by the Free Software Foundation, either version 3 of the License, or (at
*  your option) any later version.
*
*  GuardianesBA is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
*  Public License for more details.
*
*  You should have received a copy of the GNU General Public License along
*  with GuardianesBA. If not, see <https://www.gnu.org/licenses/>.
**/
package us.dit.service.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.api.score.ScoreManager; 
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;

import us.dit.service.model.entities.Calendar; 
import us.dit.service.model.entities.*;
import us.dit.service.model.entities.primarykeys.CalendarPK;
import us.dit.service.model.repositories.ShiftRepository;
import us.dit.service.model.repositories.CalendarRepository;
import us.dit.service.model.repositories.ScheduleRepository;
import us.dit.service.model.repositories.DoctorRepository;
import javax.persistence.EntityManager;

/**
 * Service responsible for building the problem dataset and 
 * invoking the OptaPlanner solver to generate the schedule.
 * * @author josperart3
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

    private static final int GUARDIAS_POR_DIA = 2; 
    private static final int SHIFTS_BASE_POR_LABORABLE = 2; // Mínimo base, pero puede subir

    @Transactional(timeout = 900)
    public Schedule solveProblem(YearMonth ym) {
        log.info(">>> 1. INICIO OptaplannerGuardians.solveProblem para {}", ym);

        // Construir y GUARDAR el problema inicial con capacidad calculada
        Schedule managedProblem = buildAndSaveInitialProblem(ym);
        
        log.info(">>> 2. Problema persistido. Iniciando Solver...");

        SolverFactory<Schedule> factory = SolverFactory.createFromXmlResource("solver/guardianesSolverConfig.xml");
        Solver<Schedule> solver = factory.buildSolver();

        //  Resolver
        Schedule bestSolution = solver.solve(managedProblem);

        // --- DIAGNÓSTICO DEL SCORE ---
        ScoreManager<Schedule> scoreManager = ScoreManager.create(factory);
        log.info("--- EXPLICACIÓN DEL SCORE ---");
        log.info(scoreManager.explainScore(bestSolution));
        // -----------------------------

        log.info(">>> 3. Resolución completada! Score: {}", bestSolution.getScore());

        // Actualizar visualización
        updateScheduleWithSolution(managedProblem, bestSolution);

        // Guardar cambios finales
        return this.scheduleRepository.saveAndFlush(managedProblem);
    }

    private Schedule buildAndSaveInitialProblem(YearMonth ym) {
        CalendarPK pk = new CalendarPK(ym.getMonthValue(), ym.getYear());

        // --- LIMPIEZA ---
        cleanOldData(ym);

        Calendar cal = this.calendarRepository.findById(pk)
                .orElseThrow(() -> new RuntimeException("No se encontró calendario"));

        // --- ANÁLISIS DE DEMANDA (MÉDICOS) ---
        List<Doctor> allDoctors = this.doctorRepository.findAll().stream()
                .filter(d -> d.getStatus() == Doctor.DoctorStatus.AVAILABLE && d.getShiftConfiguration() != null)
                .collect(Collectors.toList());

        // Demanda de Consultas
        int totalConsultasNecesarias = allDoctors.stream()
                .mapToInt(d -> d.getShiftConfiguration().getNumConsultations())
                .sum();
        
        //  Demanda de Shifts (Cont. Asist) - ¡LA CLAVE DE TU CAMBIO!
        int totalMinShiftsNecesarios = allDoctors.stream()
                .mapToInt(d -> d.getShiftConfiguration().getMinShifts())
                .sum();

        // --- PREPARACIÓN DE DÍAS ---
        List<DayConfiguration> workingDays = new ArrayList<>();
        // Mapa para controlar cuántos Shifts (Tarde) se crean cada día
        Map<Integer, Integer> shiftsPerDayMap = new HashMap<>();

        for (DayConfiguration dc : cal.getDayConfigurations()) {
            // Inicializamos a 0
            shiftsPerDayMap.put(dc.getDay(), 0); 
            
            if (dc.getIsWorkingDay()) {
                workingDays.add(dc);
                // Por defecto, ponemos la base (2)
                shiftsPerDayMap.put(dc.getDay(), SHIFTS_BASE_POR_LABORABLE);
            }
        }

        // --- AJUSTE DE OFERTA VS DEMANDA (SHIFTS) ---
        int capacidadBaseTotal = workingDays.size() * SHIFTS_BASE_POR_LABORABLE;
        
        log.info("ANÁLISIS DE CAPACIDAD: Se necesitan mínimo {} huecos de Cont. Asist. La base (2/día) ofrece {}.", 
                totalMinShiftsNecesarios, capacidadBaseTotal);

        if (totalMinShiftsNecesarios > capacidadBaseTotal) {
            int deficit = totalMinShiftsNecesarios - capacidadBaseTotal;
            log.info(">>> DÉFICIT DETECTADO: Faltan {} huecos. Se añadirán dinámicamente.", deficit);
            
            // Repartimos el déficit entre los días laborables aleatoriamente
            Collections.shuffle(workingDays); 
            int i = 0;
            while (deficit > 0) {
                DayConfiguration dc = workingDays.get(i % workingDays.size());
                int current = shiftsPerDayMap.get(dc.getDay());
                
                // Añadimos un hueco extra a este día
                shiftsPerDayMap.put(dc.getDay(), current + 1);
                
                deficit--;
                i++;
            }
        }

        // --- CREACIÓN FÍSICA DE LOS OBJETOS ---
        Schedule sch = new Schedule();
        sch.setId(pk);
        sch.setCalendar(cal);
        sch.setStatus(Schedule.ScheduleStatus.BEING_GENERATED);
        sch.setConstraintConfiguration(new us.dit.service.model.entities.score.GuardianesConstraintConfiguration(0L));
        
        // Inicializar listas para evitar NPE en Hibernate
        sch.setShiftList(new ArrayList<>());
        sch.setShiftAssignments(new ArrayList<>());
        sch.setDoctorList(new ArrayList<>());
        sch.setDayConfigurationList(new ArrayList<>());

        Schedule savedSchedule = this.scheduleRepository.saveAndFlush(sch);
        List<Shift> shiftsToSave = new ArrayList<>();

        // Bucle principal de creación
        for (DayConfiguration dc : cal.getDayConfigurations()) {
            
            // Guardias (Fijo: 2 por día)
            for (int k = 0; k < GUARDIAS_POR_DIA; k++) {
                shiftsToSave.add(createShift(dc, "GUARDIA", false, false));
            }

            // Shifts / Cont. Asist (Dinámico según el mapa calculado arriba)
            // Si es festivo, el mapa devolverá 0. Si es laborable, devolverá 2, 3, o los que toquen.
            int numShiftsHoy = shiftsPerDayMap.getOrDefault(dc.getDay(), 0);
            for (int k = 0; k < numShiftsHoy; k++) {
                shiftsToSave.add(createShift(dc, "TARDE", false, false));
            }
        }

        // Consultas (Distribución en huecos libres laborables)
        // Re-barajamos para que no coincida necesariamente con donde pusimos los shifts extra
        Collections.shuffle(workingDays);
        int consultasCreadas = 0;
        int dayIndex = 0;
        
        while (consultasCreadas < totalConsultasNecesarias && !workingDays.isEmpty()) {
            DayConfiguration dc = workingDays.get(dayIndex % workingDays.size());
            shiftsToSave.add(createShift(dc, "CONSULTA", false, true));
            consultasCreadas++;
            dayIndex++;
        }

        List<Shift> managedShifts = this.shiftRepository.saveAll(shiftsToSave);
        this.shiftRepository.flush();

        // --- VINCULACIÓN FINAL (Hibernate Safe) ---
        
        List<ShiftAssignment> newAssignments = managedShifts.stream()
                .map(s -> {
                    ShiftAssignment sa = new ShiftAssignment(s);
                    sa.setSchedule(savedSchedule); 
                    return sa;
                }).collect(Collectors.toList());
        
        // Usar la colección existente
        if(savedSchedule.getShiftAssignments() == null) savedSchedule.setShiftAssignments(new ArrayList<>());
        savedSchedule.getShiftAssignments().clear();
        savedSchedule.getShiftAssignments().addAll(newAssignments);

        if(savedSchedule.getShiftList() == null) savedSchedule.setShiftList(new ArrayList<>());
        savedSchedule.getShiftList().clear();
        savedSchedule.getShiftList().addAll(managedShifts);

        if(savedSchedule.getDoctorList() == null) savedSchedule.setDoctorList(new ArrayList<>());
        savedSchedule.getDoctorList().clear();
        savedSchedule.getDoctorList().addAll(allDoctors);

        if(savedSchedule.getDayConfigurationList() == null) savedSchedule.setDayConfigurationList(new ArrayList<>());
        savedSchedule.getDayConfigurationList().clear();
        savedSchedule.getDayConfigurationList().addAll(cal.getDayConfigurations());

        return this.scheduleRepository.saveAndFlush(savedSchedule);
    }

    private void updateScheduleWithSolution(Schedule managed, Schedule solution) {
        managed.setStatus(Schedule.ScheduleStatus.PENDING_CONFIRMATION);
        managed.setScore(solution.getScore());
        
        if (managed.getDays() == null) managed.setDays(new TreeSet<>());

        if (managed.getDays().isEmpty()) {
            for (DayConfiguration dc : managed.getDayConfigurationList()) {
                ScheduleDay sd = new ScheduleDay();
                sd.setDay(dc.getDay());
                sd.setMonth(managed.getMonth());
                sd.setYear(managed.getYear());
                sd.setIsWorkingDay(dc.getIsWorkingDay());
                sd.setSchedule(managed);
                sd.setCycle(new ArrayList<>());
                sd.setShifts(new ArrayList<>());
                sd.setConsultations(new ArrayList<>());
                managed.getDays().add(sd);
            }
        }

        Map<Long, ShiftAssignment> solutionMap = solution.getShiftAssignments().stream()
                .collect(Collectors.toMap(sa -> sa.getShift().getId(), sa -> sa));

        for (ShiftAssignment managedSa : managed.getShiftAssignments()) {
            ShiftAssignment solvedSa = solutionMap.get(managedSa.getShift().getId());
            
            if (solvedSa != null && solvedSa.getDoctor() != null) {
                managedSa.setDoctor(solvedSa.getDoctor());
                
                int day = managedSa.getDayConfiguration().getDay();
                ScheduleDay sd = managed.getDays().stream().filter(d -> d.getDay() == day).findFirst().orElse(null);
                
                if (sd != null) {
                    if (sd.getCycle() == null) sd.setCycle(new ArrayList<>());
                    if (sd.getShifts() == null) sd.setShifts(new ArrayList<>());
                    if (sd.getConsultations() == null) sd.setConsultations(new ArrayList<>());

                    Doctor doc = solvedSa.getDoctor();
                    switch (managedSa.getShift().getShiftType()) {
                        case "GUARDIA": sd.getCycle().add(doc); break;
                        case "CONSULTA": sd.getConsultations().add(doc); break;
                        case "TARDE": sd.getShifts().add(doc); break;
                    }
                }
            }
        }
    }

    private void cleanOldData(YearMonth ym) {
        entityManager.createQuery("DELETE FROM ShiftAssignment sa WHERE sa.schedule.month = :m AND sa.schedule.year = :y")
            .setParameter("m", ym.getMonthValue()).setParameter("y", ym.getYear()).executeUpdate();
        entityManager.createQuery("DELETE FROM ScheduleDay sd WHERE sd.month = :m AND sd.year = :y")
            .setParameter("m", ym.getMonthValue()).setParameter("y", ym.getYear()).executeUpdate();
        entityManager.createQuery("DELETE FROM Schedule s WHERE s.month = :m AND s.year = :y")
            .setParameter("m", ym.getMonthValue()).setParameter("y", ym.getYear()).executeUpdate();
        scheduleRepository.flush();
        entityManager.clear();
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