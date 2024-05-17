package us.dit.service.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.client.UserTaskServicesClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import us.dit.service.model.entities.Calendar;
import us.dit.service.model.entities.DayConfiguration;
import us.dit.service.model.repositories.CalendarRepository;
import us.dit.service.model.repositories.DoctorRepository;

import javax.servlet.http.HttpSession;
import javax.validation.Validator;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio que obtiene, reclama y completa la tarea de calendario
 * Ademas crea el objeto Calendar para añadir los festivos
 */
@Service
public class CalendarTaskService {

    private static final Logger logger = LogManager.getLogger();

    private final String CalendarTaskName = "Establecer festivos";
    @Autowired
    private KieUtilService kieUtils;

    @Autowired
    private TasksService tasksService;
    @Value("${kieserver.containerId}")
    private String containerId;
    @Value("${kieserver.processId}")
    private String processId;
    @Autowired
    private CalendarRepository calendarRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private Validator validator;

    /**
     * Metodo que filtra las tareas y obtiene la tarea de calendario
     *
     * @param session   objeto que maneja la sesion HTTP
     * @param principal Cadena que representa al usuario
     */
    public void obtainCalendarTask(HttpSession session, String principal) {
        logger.info("Obtenemos la tarea de calendario");
        List<TaskSummary> taskList = tasksService.findPotential(principal);
        logger.info("Las tareas son " + taskList);

        List<TaskSummary> tasksFilteredByName = taskList.stream()
                .filter(task -> task.getName().equals(CalendarTaskName)
                        && task.getProcessId().equals(processId)
                        && (task.getStatus().equals("Ready")||task.getStatus().equals("InProgress")))
                .collect(Collectors.toList());

        logger.info("Las tareas filtradas son " + tasksFilteredByName);
        if (!tasksFilteredByName.isEmpty()) {
            logger.info("Hay tareas de calendario disponibles");
            long calendarTaskId = tasksFilteredByName.get(0).getId();
            logger.info("El id de la tarea es " + calendarTaskId);
            session.setAttribute("tareaId", calendarTaskId);
        }

    }

    /**
     * Metodo que reclama la tarea de calendario para que la realice el usuario
     * y la completa cuando se ha realizado
     *
     * @param principal Cadena que representa al usuario
     * @param festivos  Set de LocalDate que representa los festivos
     * @param taskId    Representa el id de la tarea
     */
    public void initAndCompleteCalendarTask(String principal, Set<LocalDate> festivos, Long taskId) {
        UserTaskServicesClient userClient = kieUtils.getUserTaskServicesClient();
        if(userClient.findTaskById(taskId).getStatus().equals("Ready"))
        {
	        logger.info("Reclamamos la tarea de calendario con id " + taskId);
	        userClient.claimTask(containerId, taskId, principal);
	
	        logger.info("Comenzamos el completado de la tarea de calendario con id " + taskId);
	        userClient.startTask(containerId, taskId, principal);
        }
        logger.info("Construimos el calendario");
        Calendar calendarioFestivos = this.buildCalendar(festivos);

        logger.info("Persistimos el calendario " + calendarioFestivos);
        this.calendarRepository.save(calendarioFestivos);

        logger.info("Construimos el mapa con los parametros de salida");
        Map<String, Object> params = new HashMap<>();
        String idCalendarioFestivos = calendarioFestivos.getMonth() + "-" + calendarioFestivos.getYear();
        params.put("Id_Calendario_Festivos", idCalendarioFestivos);
        logger.info("Se termina la tarea de Establecer festivos");
        userClient.completeTask(containerId, taskId, principal, params);

    }

    /**
     * Metodo que construye el objeto Calendar y le añade los festivos introducidos por el usuario
     *
     * @param festivos Set de LocalDate que representa los festivos
     * @return El objeto calendar
     */
    private Calendar buildCalendar(Set<LocalDate> festivos) {
        logger.info("Los festivos son {} ", festivos);
        LocalDate yearAndMonth = festivos.iterator().next();
        YearMonth yearMonth = YearMonth.of(yearAndMonth.getYear(), yearAndMonth.getMonth());
        Calendar calendar = new Calendar(yearMonth.getMonthValue(), yearMonth.getYear());
        List<DayConfiguration> dayConfs = new LinkedList<>();
        DayConfiguration dayConf;
        LocalDate currDate = yearMonth.atDay(1);
        DayOfWeek currDayWeek = currDate.getDayOfWeek();
        while (currDate.getMonth().equals(yearMonth.getMonth())) {
            logger.info("El dia actual es " + currDate);
            dayConf = new DayConfiguration();
            dayConf.setDay(currDate.getDayOfMonth());
            dayConf.setIsWorkingDay(currDayWeek != DayOfWeek.SATURDAY
                    && currDayWeek != DayOfWeek.SUNDAY
                    && !festivos.contains(currDate));
            dayConf.setNumShifts(2);
            dayConf.setNumConsultations(0);
            dayConf.setCalendar(calendar);
            dayConfs.add(dayConf);

            currDate = currDate.plusDays(1);
            currDayWeek = currDate.getDayOfWeek();
        }
        calendar.setDayConfigurations(new TreeSet<>(dayConfs));
        return calendar;
    }


}
