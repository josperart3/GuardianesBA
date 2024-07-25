/**
 * This file is part of GuardianesBA - Business Application for processes managing healthcare tasks planning and supervision.
 * Copyright (C) 2024  Universidad de Sevilla/Departamento de Ingeniería Telemática
 * <p>
 * GuardianesBA is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * <p>
 * GuardianesBA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with GuardianesBA. If not, see <https://www.gnu.org/licenses/>.
 **/
package us.dit.service.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.dit.service.model.entities.Calendar;
import us.dit.service.model.entities.Schedule;
import us.dit.service.model.entities.Schedule.ScheduleStatus;
import us.dit.service.model.entities.primarykeys.CalendarPK;
import us.dit.service.model.repositories.CalendarRepository;
import us.dit.service.model.repositories.ScheduleRepository;
import us.dit.service.services.SchedulerService;

import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Esta clase es el WIH para atender a la tarea GenerarPlanificacion definida en el proceso
 *
 * @author Jose Carlos Rodríguez Morón
 * @version 1.0
 * @date Julio 2024
 */
@Component("GenerarPlanificacion")
public class GenerateScheduleWorkItemHandler implements WorkItemHandler {
    private static final Logger logger = LogManager.getLogger();
    @Autowired
    private SchedulerService schedulerService;
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private CalendarRepository calendarRepository;

    @Override
    public void executeWorkItem(WorkItem workItem, WorkItemManager workItemManager) {
        String idCalendarioFestivos = (String) workItem.getParameter("Id_calendario_festivos");

        logger.info("Ejecutando WorkItemHandler para el trabajo: " + workItem.getName());

        String[] parts = idCalendarioFestivos.split("-");
        int month = Integer.parseInt(parts[0]);
        int year = Integer.parseInt(parts[1]);

        //Asi el yearMonth construido es del mes siguiente y es el obtenido de la tarea Establecer festivos
        YearMonth yearMonth = YearMonth.of(year, month);

        logger.info("Request received to generate schedule for: " + yearMonth);
        //Y ademas como construimos en la tarea establecer festivos el calendario
        //podemos recuperarlo y poder usar su scheduler de forma sencilla
        CalendarPK pk = new CalendarPK(yearMonth.getMonthValue(), yearMonth.getYear());

        logger.info("El CalendarPK generado es " + pk);

        Optional<Calendar> calendar = this.calendarRepository.findById(pk);
        if (!calendar.isPresent()) {
            throw new RuntimeException("Trying to generate a schedule for a non existing calendar");
        }

        if (this.scheduleRepository.findById(pk).isPresent()) {
            throw new RuntimeException("The schedule is already generated");
        }
        logger.info("Persisting a schedule with status " + ScheduleStatus.BEING_GENERATED);
        Schedule schedule = new Schedule(ScheduleStatus.BEING_GENERATED);
        schedule.setCalendar(calendar.get());
        this.scheduleRepository.save(schedule);

        this.schedulerService.startScheduleGeneration(calendar.get());

        // Recuperamos el schedule para guardar su id en el proceso que sera el mes y año de ese calendario
        int scheduleMonth = this.scheduleRepository.findById(pk).get().getMonth();
        int scheduleYear = this.scheduleRepository.findById(pk).get().getYear();
        // Para posteriormente en la tarea Validar planificacion podamos obtener el schedule
        String idPlanificacionProvisional = scheduleMonth + "-" + scheduleYear;
        logger.info("El id de la planificacion es " + idPlanificacionProvisional);
        Map<String, Object> results = new HashMap<>();
        results.put("Id_planficacion_provisional", idPlanificacionProvisional);
        logger.info("Se termina la tarea de Generar Planificacion");
        workItemManager.completeWorkItem(workItem.getId(), results);

    }

    @Override
    public void abortWorkItem(WorkItem workItem, WorkItemManager workItemManager) {

    }
}
