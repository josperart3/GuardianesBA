
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
package us.dit.service.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.context.annotation.Lazy;
import us.dit.service.model.ScheduleView;
import us.dit.service.model.entities.Schedule;
import us.dit.service.services.SchedulerTaskService;

import javax.servlet.http.HttpSession;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
/**
 * Esta clase es el controlador para manejar el generador de planificaciones
 *
 * @author Jose Carlos Rodríguez Morón
 * @version 1.0
 * @date Julio 2024
 */
@Lazy
@Controller
@RequestMapping("/guardianes")
public class ScheduleController {

    private static final Logger logger = LogManager.getLogger();
    private static final String SCHEDULE_ATTR = "schedule";
    private static final String USE_LIST_ATTR = "useListView";
    private static final String YEAR_MONTH_ATTR = "yearMonth";

    @Autowired
    private SchedulerTaskService schedulerTaskService;

    /**
     * Este método reclamará la tarea de validar planificacion y devolverá el html de la planificación
     *
     * @param session objeto que maneja la sesion HTTP
     * @return El html de la planificación
     */
    @GetMapping("/schedules")
    public String getSchedule(HttpSession session, @RequestParam(defaultValue = "false") Boolean useListView, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetails principal = (UserDetails) auth.getPrincipal();
        logger.debug("Datos del usuario principal" + principal);
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        if (roles.contains("ROLE_GESTOR")) {
            this.schedulerTaskService.obtainValidateScheduleTask(session, principal.getUsername());
            logger.info("Tarea validar planificacion iniciada");
            String planificacionProvisional = this.schedulerTaskService.obtainInputParameters((Long) session.getAttribute("tareaValidarPlanificacionId"));
            String[] yearMonthString = this.schedulerTaskService.obtainYearMonth(planificacionProvisional);
            YearMonth yearMonth = YearMonth.of(Integer.parseInt(yearMonthString[1]), Integer.parseInt(yearMonthString[0]));
            Optional<Schedule> schedule = this.schedulerTaskService.obtainSchedule(yearMonth);
            if (schedule.isPresent()) {
                ScheduleView scheduleView = this.schedulerTaskService.setView(schedule.get(), yearMonth);
                model.addAttribute(SCHEDULE_ATTR, scheduleView);
                model.addAttribute(YEAR_MONTH_ATTR, yearMonth.toString());
                model.addAttribute(USE_LIST_ATTR, useListView);
            }
            logger.info("Devolvemos el html de la planificacion");
            return "schedule";
        } else {
            logger.info("Devolvemos el html de error");
            return "error";
        }
    }

    @PostMapping("/schedules")
    public String confirmSchedule(HttpSession session, @RequestParam(defaultValue = "false") Boolean confirmation) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetails principal = (UserDetails) auth.getPrincipal();
        logger.debug("Datos de usuario (principal)" + principal);
        if (confirmation) {
            logger.info("El usuario ya ha confirmado la planificacion");
            this.schedulerTaskService.initAndCompleteValidateScheduleTask(principal.getUsername(),
                    (Long) session.getAttribute("tareaValidarPlanificacionId"));
        }
        return "redirect:/";
    }
}
