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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.client.UserTaskServicesClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import us.dit.service.model.ScheduleView;
import us.dit.service.model.entities.Schedule;
import us.dit.service.model.entities.ScheduleDay;
import us.dit.service.model.entities.primarykeys.CalendarPK;
import us.dit.service.model.repositories.ScheduleRepository;

import javax.servlet.http.HttpSession;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
/**
 *
 * @author Jose Carlos Rodríguez Morón
 * @version 1.0
 * @date Julio 2024
 */
@Lazy
@Service
public class SchedulerTaskService {

    private static final Logger logger = LogManager.getLogger();

    private final String ValidateScheduleTaskName = "Validar planificación";

    @Autowired
    private KieUtilService kieUtils;

    @Autowired
    private TasksService tasksService;
    @Value("${kieserver.containerId}")
    private String containerId;
    @Value("${kieserver.processId}")
    private String processId;

    @Value("${guardianes.jbpm.validarPlanificacion.inputContent}")
    private String inputContentId;

    @Autowired
    private ScheduleRepository scheduleRepository;

    /**
     * Metodo que filtra las tareas y obtiene la tarea de validar planificacion
     *
     * @param session   objeto que maneja la sesion HTTP
     * @param principal Cadena que representa al usuario
     */
    public void obtainValidateScheduleTask(HttpSession session, String principal) {
        logger.info("Obtenemos la tarea de validar planificacion");
        List<TaskSummary> taskList = this.tasksService.findPotential(principal);
        logger.debug("Las tareas son " + taskList);

        List<TaskSummary> tasksFilteredByName = taskList.stream()
                .filter(task -> task.getName().equals(this.ValidateScheduleTaskName)
                        && task.getProcessId().equals(this.processId)
                        && task.getStatus().equals("Ready") || task.getStatus().equals("InProgress"))
                .collect(Collectors.toList());

        logger.info("Las tareas filtradas son " + tasksFilteredByName);
        if (!tasksFilteredByName.isEmpty()) {
            logger.debug("Hay tareas de validar planificacion disponibles");
            long validateScheduleTaskId = tasksFilteredByName.get(0).getId();
            logger.debug("El id de la tarea es " + validateScheduleTaskId);
            session.setAttribute("tareaValidarPlanificacionId", validateScheduleTaskId);
        }
    }

    public void initAndCompleteValidateScheduleTask(String principal, Long taskId) {
        UserTaskServicesClient userClient = kieUtils.getUserTaskServicesClient();
        if (userClient.findTaskById(taskId).getStatus().equals("Ready")) {
            logger.info("Reclamamos la tarea de validar planificacion con id " + taskId);
            userClient.claimTask(containerId, taskId, principal);
            logger.debug("Comenzamos el completado de la tarea de validar planificacion con id " + taskId);
            userClient.startTask(containerId, taskId, principal);
        }
        String yearMonthString = this.obtainInputParameters(taskId);
        String[] splitYearMonth = this.obtainYearMonth(yearMonthString);
        YearMonth yearMonth = YearMonth.of(Integer.parseInt(splitYearMonth[1]), Integer.parseInt(splitYearMonth[0]));
        Optional<Schedule> schedule = this.obtainSchedule(yearMonth);
        if (schedule.isPresent()) {
            if (schedule.get().getStatus() == Schedule.ScheduleStatus.PENDING_CONFIRMATION) {
                schedule.get();
            } else {
                throw new RuntimeException("Estado de la planificacion no valido");
            }
            schedule.get().setStatus(Schedule.ScheduleStatus.CONFIRMED);
            this.scheduleRepository.save(schedule.get());
        }

        Map<String, Object> params = new HashMap<>();
        params.put("Id_planificacion_valida", yearMonthString);
        logger.info("Se termina la tarea de Validar planificacion");
        userClient.completeTask(containerId, taskId, principal, params);
    }

    public String obtainInputParameters(Long taskId) {
        UserTaskServicesClient userClient = kieUtils.getUserTaskServicesClient();
        Map<String, Object> inputTaskContent = userClient.getTaskInputContentByTaskId(this.containerId, taskId);
        logger.info("Devolvemos los parámetros de entrada " + inputTaskContent.get(this.inputContentId));
        return String.valueOf(inputTaskContent.get(this.inputContentId));
    }

    public String[] obtainYearMonth(String idPlanificacionProvisional) {
        return idPlanificacionProvisional.split("-");
    }

    public Optional<Schedule> obtainSchedule(YearMonth yearMonth) {
        CalendarPK calendarPK = new CalendarPK(yearMonth.getMonthValue(), yearMonth.getYear());
        return this.scheduleRepository.findById(calendarPK);
    }

    public ScheduleView setView(Schedule schedule, YearMonth yearMonth) {
        ScheduleView scheduleView = new ScheduleView();
        scheduleView.setYear(yearMonth.getYear());
        scheduleView.setMonth(yearMonth.getMonthValue());
        scheduleView.setDays(schedule.getDays());
        scheduleView.setStatus(schedule.getStatus().name());
        logger.info("Mapping the schedule days to the weeks list");
        List<List<ScheduleDay>> weeks = new LinkedList<>();
        List<ScheduleDay> currWeek = new ArrayList<>(7);
        LocalDate dayOfMonth = yearMonth.atDay(1);
        // Fill the currWeek list until the dayOfMonth
        // E.g. if dayOfMonth is a Wednesday, two days have to be added (previous Monday
        // and Tuesday)
        int daysToAdd = dayOfMonth.getDayOfWeek().getValue() - 1;
        logger.debug("The first day of the month has value: " + dayOfMonth.getDayOfWeek().getValue());
        logger.debug("Number of days to be added before the first day of the month: " + daysToAdd);
        for (int i = 0; i < daysToAdd; i++) {
            int day = dayOfMonth.minusDays(daysToAdd - i).getDayOfMonth();
            currWeek.add(createEmtpyScheduleDay(day));
        }
        logger.debug("The current week after adding the days before the first day of the month is: " + currWeek);
        // Now, fill the weeks list with the real schedule days
        for (ScheduleDay scheduleDay : schedule.getDays()) {
            if (scheduleDay.getShifts().isEmpty()) {
                scheduleDay.setShifts(Collections.emptyList());
            }
            if (scheduleDay.getConsultations().isEmpty()) {
                scheduleDay.setConsultations(Collections.emptyList());
            }
            currWeek.add(scheduleDay);

            if (dayOfMonth.getDayOfWeek() == DayOfWeek.SUNDAY) {
                logger.debug("Adding week: " + currWeek);
                logger.debug("The number of days being added is: " + currWeek.size());
                weeks.add(currWeek);
                currWeek = new ArrayList<>(7);
            }
            dayOfMonth = dayOfMonth.plusDays(1);
        }
        dayOfMonth = yearMonth.atEndOfMonth();
        // Now we add more days until reaching a Sunday
        daysToAdd = 7 - dayOfMonth.getDayOfWeek().getValue();
        logger.debug("Number of days to be added after the last day of the month: " + daysToAdd);
        for (int i = 0; i < daysToAdd; i++) {
            int day = dayOfMonth.plusDays(i + 1).getDayOfMonth();
            currWeek.add(createEmtpyScheduleDay(day));
        }
        if (daysToAdd > 0) {
            logger.debug("The current week after adding the days after the last day of the month is: " + currWeek);
            logger.debug("The number of days being added is: " + currWeek.size());
            weeks.add(currWeek);
        }
        logger.debug("The created list of weeks is: " + weeks);
        scheduleView.setWeeks(weeks);
        return scheduleView;
    }

    private ScheduleDay createEmtpyScheduleDay(Integer day) {
        logger.debug("Request to create an emtpy schedule day for: " + day);
        ScheduleDay scheduleDay = new ScheduleDay();
        scheduleDay.setDay(day);
        scheduleDay.setIsWorkingDay(false);
        scheduleDay.setCycle(null);
        scheduleDay.setShifts(null);
        scheduleDay.setConsultations(null);
        logger.debug("The created schedule day is: " + scheduleDay);
        return scheduleDay;
    }
}
