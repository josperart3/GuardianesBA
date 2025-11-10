package us.dit.service.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.solver.SolverJob;
import org.optaplanner.core.api.solver.SolverManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import us.dit.service.services.SchedulerService;
import us.dit.service.model.entities.AllowedShift;
import us.dit.service.model.entities.Calendar;
import us.dit.service.model.entities.DayConfiguration;
import us.dit.service.model.entities.Doctor;
import us.dit.service.model.entities.Rol;
import us.dit.service.model.entities.Schedule;
import us.dit.service.model.entities.Schedule.ScheduleStatus;
import us.dit.service.model.entities.Shift;
import us.dit.service.model.entities.ShiftConfiguration;
import us.dit.service.model.entities.primarykeys.CalendarPK;
import us.dit.service.model.repositories.AllowedShiftRepository;
import us.dit.service.model.repositories.CalendarRepository;
import us.dit.service.model.repositories.DayConfigurationRepository;
import us.dit.service.model.repositories.DoctorRepository;
import us.dit.service.model.repositories.RolRepository;
import us.dit.service.model.repositories.ScheduleRepository;
import us.dit.service.model.repositories.ShiftConfigurationRepository;
import us.dit.service.model.repositories.ShiftRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;


class SchedulerServiceUnitTest {

    @Mock private ScheduleRepository scheduleRepository;
    @Mock private ShiftRepository shiftRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private DayConfigurationRepository dayConfigurationRepository;
    @Mock private CalendarRepository calendarRepository;
    @Mock private SolverManager<Schedule, CalendarPK> solverManager;
    @Mock private SolverJob<Schedule, CalendarPK> solverJob;

    @InjectMocks private SchedulerService schedulerService;

    private CalendarPK pk;
    private Calendar calendar;
    private Schedule working;
    private Schedule solved;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        pk = new CalendarPK(8, 2025);
        calendar = new Calendar();
        calendar.setMonth(8);
        calendar.setYear(2025);

        working = new Schedule();
        working.setCalendar(calendar);
        working.setStatus(Schedule.ScheduleStatus.BEING_GENERATED);

        solved = new Schedule();
        solved.setCalendar(calendar);
        solved.setStatus(Schedule.ScheduleStatus.BEING_GENERATED);
        solved.setShiftAssignments(new ArrayList<>());
        solved.setScore(HardSoftScore.of(0, 10));
    }

    @Test
    void startScheduleGeneration_happyPath_persistsBestSolution() throws InterruptedException, ExecutionException {
        // repos
        when(calendarRepository.findById(pk)).thenReturn(Optional.of(calendar));
        when(scheduleRepository.findById(pk)).thenReturn(Optional.of(working));
        when(solverManager.solve(eq(pk), any(Schedule.class))).thenReturn(solverJob);
        when(solverJob.getFinalBestSolution()).thenReturn(solved);

        // problem facts:
        when(doctorRepository.findAll()).thenReturn(Arrays.asList(new Doctor()));
        when(dayConfigurationRepository.findByCalendarMonthAndCalendarYear(8, 2025)).thenReturn(Arrays.asList(new DayConfiguration()));
        when(shiftRepository.findByDayConfigurationCalendarMonthAndDayConfigurationCalendarYear(8, 2025)).thenReturn(Arrays.asList(new Shift()));

        schedulerService.startScheduleGeneration(calendar);

        // Se guarda el working y luego el best
        verify(scheduleRepository, atLeast(1)).saveAndFlush(any(Schedule.class));
        verify(scheduleRepository, atLeast(1)).saveAndFlush(argThat(s -> s.getStatus() == Schedule.ScheduleStatus.PENDING_CONFIRMATION));
        verify(solverManager).solve(eq(pk), any(Schedule.class));
    }

    @Test
    void startScheduleGeneration_calendarNull_throwsNPE() {
        assertThrows(NullPointerException.class, () -> schedulerService.startScheduleGeneration(null));
        verifyNoInteractions(solverManager);
    }

    @Test
    void startScheduleGeneration_calendarNotInDb_illegalArgument() {
        when(calendarRepository.findById(pk)).thenReturn(Optional.empty());
        Calendar detached = new Calendar();
        detached.setMonth(8); detached.setYear(2025);
        assertThrows(IllegalArgumentException.class, () -> schedulerService.startScheduleGeneration(detached));
        verifyNoInteractions(solverManager);
    }

    @Test
    void startScheduleGeneration_solverInterrupted_setsErrorStatus() throws InterruptedException, ExecutionException {
        when(calendarRepository.findById(pk)).thenReturn(Optional.of(calendar));
        when(scheduleRepository.findById(pk)).thenReturn(Optional.of(working));

        when(doctorRepository.findAll()).thenReturn(Arrays.asList());
        when(dayConfigurationRepository.findByCalendarMonthAndCalendarYear(8, 2025)).thenReturn(Arrays.asList());
        when(shiftRepository.findByDayConfigurationCalendarMonthAndDayConfigurationCalendarYear(8, 2025)).thenReturn(Arrays.asList());

        when(solverManager.solve(eq(pk), any(Schedule.class))).thenReturn(solverJob);
        when(solverJob.getFinalBestSolution()).thenThrow(new InterruptedException("boom"));

        schedulerService.startScheduleGeneration(calendar);

        verify(scheduleRepository, atLeastOnce()).save(argThat(s -> s.getStatus() == Schedule.ScheduleStatus.GENERATION_ERROR));
    }

    @Test
    void startScheduleGeneration_solverExecutionException_setsErrorStatus() throws InterruptedException, ExecutionException {
        when(calendarRepository.findById(pk)).thenReturn(Optional.of(calendar));
        when(scheduleRepository.findById(pk)).thenReturn(Optional.of(working));

        when(doctorRepository.findAll()).thenReturn(Arrays.asList());
        when(dayConfigurationRepository.findByCalendarMonthAndCalendarYear(8, 2025)).thenReturn(Arrays.asList());
        when(shiftRepository.findByDayConfigurationCalendarMonthAndDayConfigurationCalendarYear(8, 2025)).thenReturn(Arrays.asList());

        when(solverManager.solve(eq(pk), any(Schedule.class))).thenReturn(solverJob);
        when(solverJob.getFinalBestSolution()).thenThrow(new ExecutionException("x", new RuntimeException("y")));

        schedulerService.startScheduleGeneration(calendar);

        verify(scheduleRepository, atLeastOnce()).save(argThat(s -> s.getStatus() == Schedule.ScheduleStatus.GENERATION_ERROR));
    }
}