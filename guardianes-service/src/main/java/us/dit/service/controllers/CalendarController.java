
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
import org.kie.server.api.model.instance.TaskSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import us.dit.service.services.CalendarTaskService;
import us.dit.service.services.JsonParserFestivos;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Esta clase es el controlador para manejar los calendarios
 *
 * @author Jose Carlos Rodríguez Morón
 * @version 1.0
 * @date Julio 2024
 */
@Controller
@RequestMapping("/guardianes")
public class CalendarController {

    private static final Logger logger = LogManager.getLogger();

    @Autowired
    private CalendarTaskService calendarTaskService;

    /**
     * Este método mostrará el menú de calendario
     *
     * @param session objeto que maneja la sesion HTTP
     * @return El html del calendario
     */
    @GetMapping("/calendars")
    public String menu(HttpSession session) {
        return this.obtenerTareaEstablecerFestivos(session);
    }

    /**
     * Este metodo es el que obtiene la tarea de calendario en funcion de quien la tenga asignada
     *
     * @param session objeto que maneja la sesion HTTP
     */
    private String obtenerTareaEstablecerFestivos(HttpSession session) {
        logger.info("Iniciando la seleccion de festivos");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetails principal = (UserDetails) auth.getPrincipal();
        logger.debug("Datos del usuario principal" + principal);
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        if (roles.contains("ROLE_ADMINISTRADOR") || roles.contains("ROLE_GESTOR")) {
            //Que se inicie la tarea
            List<TaskSummary> tasksObtained = this.calendarTaskService.obtainCalendarTask(session, principal.getUsername());
            logger.info("Tarea iniciada");
            if (tasksObtained.isEmpty()) {
                logger.info("Devolvemos el html de error");
                return "error";
            }
        }
        logger.info("Devolvemos el html del calendario");
        return "calendar";
    }

    /**
     * Metodo que maneja la peticion HTTP POST cuando el usuario selecciona los festivos
     *
     * @param session          objeto que maneja la sesion HTTP
     * @param festivosResponse objeto que representa el JSON que recibimos en la peticion POST con los festivos
     * @return Redireccion a que se ha servido correctamente la peticion HTTP
     */
    @PostMapping("/calendars")
    @ResponseBody
    public String inciaryCompletarTareaEstablecerFestivos(HttpSession session, @RequestBody String festivosResponse) {
        logger.info("El usuario ya ha seleccionado los festivos");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserDetails principal = (UserDetails) auth.getPrincipal();
        logger.debug("Datos de usuario (principal)" + principal);

        Set<LocalDate> festivos = JsonParserFestivos.parseFestivos(festivosResponse);
        logger.info("Los festivos son " + festivos);

        this.calendarTaskService.initAndCompleteCalendarTask(principal.getUsername(), festivos, (Long) session.getAttribute("tareaId"));

        return "/";
    }
}
