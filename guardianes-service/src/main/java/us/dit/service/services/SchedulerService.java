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
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.api.solver.SolverJob;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import us.dit.service.model.entities.Calendar;
import us.dit.service.model.entities.DayConfiguration;
import us.dit.service.model.entities.Doctor;
import us.dit.service.model.entities.Schedule;
import us.dit.service.model.entities.Schedule.ScheduleStatus;
import us.dit.service.model.entities.Shift;
import us.dit.service.model.entities.ShiftAssignment;
import us.dit.service.model.entities.primarykeys.CalendarPK;
import us.dit.service.model.repositories.DayConfigurationRepository;
import us.dit.service.model.repositories.DoctorRepository;
import us.dit.service.model.repositories.ScheduleRepository;
import us.dit.service.model.repositories.ShiftRepository;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * This class is responsible for dealing with the communication with the
 * Scheduler system, whenever a {@link Schedule} is to be generated
 *
 * @author miggoncan
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final ScheduleRepository scheduleRepository;
    private final ShiftRepository shiftRepository;
    private final DoctorRepository doctorRepository;
    private final DayConfigurationRepository dayConfigurationRepository; 

    private final SolverManager<Schedule, CalendarPK> solverManager;

    /**
     * Genera y persiste la planificación para el {@link Calendar} indicado usando OptaPlanner 8.7.0.Final.
     * Sustituye al antiguo scheduler en Python.
     */
    @Transactional
    public void startScheduleGeneration(Calendar calendar) {
        Objects.requireNonNull(calendar, "calendar must not be null");
        final Integer month = calendar.getMonth();
        final Integer year  = calendar.getYear();
        final CalendarPK problemId = new CalendarPK(month, year);

        log.info("Iniciando generación con OptaPlanner para {}/{}", month, year);

        // 1) Cargar/crear el Schedule de trabajo (estado BEING_GENERATED)
        Schedule workingSchedule = scheduleRepository.findById(new CalendarPK(month, year))
                .orElseGet(() -> {
                    Schedule s = new Schedule();
                    s.setMonth(month);
                    s.setYear(year);
                    s.setCalendar(calendar);
                    return s;
                });

        workingSchedule.setStatus(ScheduleStatus.BEING_GENERATED);

        // 2) Poblar FACTS (rangos) y entidades planificables
        populateFactsAndEntities(workingSchedule);

        try {
	        // 3) Resolver SINCRÓNICAMENTE con SolverManager
	        //    (solve(...) bloquea hasta obtener la mejor solución)
	        SolverJob<Schedule, CalendarPK> job = solverManager.solve(problemId, workingSchedule);
	        Schedule bestSolution = job.getFinalBestSolution(); // bloquea hasta la mejor solución
	
	        // 4) Persistir la mejor solución
	        bestSolution.setStatus(ScheduleStatus.PENDING_CONFIRMATION); 
	        // Importante para JPA: asegurar que las asociaciones bidireccionales están bien
	        if (bestSolution.getShiftAssignments() != null) {
	            bestSolution.getShiftAssignments().forEach(a -> a.setSchedule(bestSolution));
	        }
	        scheduleRepository.save(bestSolution);
	
	        log.info("Planificación {}/{} generada y persistida con score={}", month, year, bestSolution.getScore());
       
        } catch(InterruptedException e) {
        	Thread.currentThread().interrupt();
            log.error("Solver interrumpido para {}/{}", month, year, e);
            workingSchedule.setStatus(ScheduleStatus.GENERATION_ERROR);
            scheduleRepository.save(workingSchedule);
        
        } catch(ExecutionException e) {
        	log.error("Error durante la resolución del solver para {}/{}", month, year, e);
            workingSchedule.setStatus(ScheduleStatus.GENERATION_ERROR);
            scheduleRepository.save(workingSchedule);
        }
    }

    /**
     * Construye las colecciones que OptaPlanner necesita dentro del Schedule:
     *  - doctorList (rango doctorRange)
     *  - shiftList (hechos del problema)
     *  - dayConfigurationList (hechos del problema)
     *  - shiftAssignments (entidades planificables)
     */
    private void populateFactsAndEntities(Schedule schedule) {
        final Integer month = schedule.getMonth();
        final Integer year  = schedule.getYear();

        // 2.1 Doctores (rango de valores @ValueRangeProvider("doctorRange"))
        // Filtra doctores eliminados/inactivos
        List<Doctor> allDoctors = doctorRepository.findAll();
        List<Doctor> doctorRange = allDoctors.stream()
                .filter(this::isAssignableDoctor)
                .collect(Collectors.toList());
        schedule.setDoctorList(doctorRange);

        // 2.2 DayConfiguration (hechos)
        List<DayConfiguration> dayConfigs = dayConfigurationRepository
                .findByCalendarMonthAndCalendarYear(month, year);

        schedule.setDayConfigurationList(dayConfigs);

        // 2.3 Shifts (hechos)
        List<Shift> shifts = shiftRepository.findByDayConfigurationCalendarMonthAndDayConfigurationCalendarYear(month, year);
        schedule.setShiftList(shifts);

        // 2.4 ShiftAssignments (entidades planificables)
        // Crea un ShiftAssignment por cada Shift si no existe ya.
        List<ShiftAssignment> existing = Optional.ofNullable(schedule.getShiftAssignments())
        	    .orElseGet(ArrayList::new);

        	Map<Long, ShiftAssignment> currentByShiftId = existing.stream()
        	    .filter(sa -> sa.getShift() != null)
        	    .collect(Collectors.toMap(
        	        (ShiftAssignment sa) -> sa.getShift().getId(),
        	        (ShiftAssignment sa) -> sa,
        	        (a, b) -> a,
        	        LinkedHashMap::new
        	    ));
        List<ShiftAssignment> assignments = new ArrayList<>(shifts.size());
        for (Shift shift : shifts) {
            ShiftAssignment saExisting = currentByShiftId.get(shift.getId());
            if (saExisting != null) {
                saExisting.setSchedule(schedule);
                assignments.add(saExisting);
            } else {
                ShiftAssignment sa = new ShiftAssignment(shift);
                sa.setSchedule(schedule);
                sa.setPinned(false);
                assignments.add(sa);
            }
        }
        schedule.setShiftAssignments(assignments);
    }


    private boolean isAssignableDoctor(Doctor d) {
        // return d.getStatus() != DoctorStatus.DELETED;
        return true;
    }
}