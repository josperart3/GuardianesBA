package us.dit.service.controllers;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.optaplanner.core.api.solver.SolverManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import us.dit.service.controllers.exceptions.CalendarNotFoundException;
import us.dit.service.model.entities.Calendar;
import us.dit.service.model.entities.Schedule;
import us.dit.service.model.entities.primarykeys.CalendarPK;
import us.dit.service.model.repositories.CalendarRepository;
import us.dit.service.model.repositories.ScheduleRepository;

@RestController
@RequestMapping("/schedule")
@Slf4j
public class PlanningController {
	@Autowired
	private ScheduleRepository scheduleRepository;
	@Autowired
	private SolverManager<Schedule,Integer> solverManager;
	@Autowired
	private CalendarRepository calendarRepository;
	
	@GetMapping
	public List<Schedule> getScheduler() {
		return scheduleRepository.findAll();
	}
	/**
	 * Estudiar el solverManager para analizar las posibilidades, configuración y lo que hace
	 * @param id
	 */
	@PostMapping("/solve/{yearMonth}")
	public void solve(@PathVariable YearMonth yearMonth) {
		log.info("Request received: solve schedule for " + yearMonth);
		CalendarPK pk = new CalendarPK(yearMonth.getMonthValue(), yearMonth.getYear());

		Optional<Calendar> calendar = calendarRepository.findById(pk);

		if (!calendar.isPresent()) {
			log.info("The calendar of the given month was not found. Throwing CalendarNotFoundException");
			throw new CalendarNotFoundException(yearMonth.getMonthValue(), yearMonth.getYear());
		}
			
		solverManager.solveAndListen(1,  
				(problemId)-> scheduleRepository.findById(pk).get(), 
				scheduleRepository::save);		
	}
	

}
