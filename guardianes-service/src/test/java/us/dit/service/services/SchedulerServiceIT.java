package us.dit.service.services;

import static org.junit.jupiter.api.Assertions.*;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import javax.transaction.Transactional;

import org.jbpm.springboot.autoconfigure.JBPMAutoConfiguration;
import org.jbpm.springboot.datasources.JBPMDataSourceAutoConfiguration;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.server.springboot.autoconfiguration.KieServerAutoConfiguration;
import org.kie.server.springboot.autoconfiguration.jbpm.CaseMgmtKieServerAutoConfiguration;
import org.kie.server.springboot.autoconfiguration.jbpm.JBPMKieServerAutoConfiguration;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import us.dit.service.model.entities.Calendar;
import us.dit.service.model.entities.DayConfiguration;
import us.dit.service.model.entities.Doctor;
import us.dit.service.model.entities.Schedule;
import us.dit.service.model.entities.Shift;
import us.dit.service.model.entities.ShiftConfiguration;
import us.dit.service.model.entities.primarykeys.CalendarPK;
import us.dit.service.model.repositories.CalendarRepository;
import us.dit.service.model.repositories.DayConfigurationRepository;
import us.dit.service.model.repositories.DoctorRepository;
import us.dit.service.model.repositories.ScheduleRepository;
import us.dit.service.model.repositories.ShiftConfigurationRepository;
import us.dit.service.model.repositories.ShiftRepository;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
        JBPMDataSourceAutoConfiguration.class,          
        JBPMAutoConfiguration.class,
        KieServerAutoConfiguration.class,
        JBPMKieServerAutoConfiguration.class,
        CaseMgmtKieServerAutoConfiguration.class,
        JmsAutoConfiguration.class
})
@TestPropertySource(properties = {
    // === DataSource H2 (test) ===
    "spring.datasource.url=jdbc:h2:mem:itdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=password",

    // === JPA/Hibernate para H2 ===
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.show-sql=false",
    "spring.jpa.properties.hibernate.format_sql=false",
    "spring.jpa.hibernate.naming.physical-strategy=org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl",

    // === Desactivar JTA/Narayana y autoconfiguraciones jbpm/kie que molestan en test ===
    "spring.jta.enabled=false",
    "app.initdata.enable=false",
    "spring.autoconfigure.exclude=org.kie.server.springboot.autoconfiguration.KieServerAutoConfiguration,org.kie.server.springboot.autoconfiguration.jbpm.JBPMKieServerAutoConfiguration,org.kie.server.springboot.autoconfiguration.jbpm.CaseMgmtKieServerAutoConfiguration,org.jbpm.springboot.autoconfigure.JBPMAutoConfiguration,org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration",

    // === OptaPlanner: límite de tiempo para que el test no se alargue ===
    "optaplanner.solver.termination.spent-limit=5s"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SchedulerServiceIT {

    @Autowired private SchedulerService schedulerService;
    @Autowired private CalendarRepository calendarRepository;
    @Autowired private DayConfigurationRepository dayConfigurationRepository;
    @Autowired private ShiftRepository shiftRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private ShiftConfigurationRepository shiftConfRepository;
    @Autowired private ScheduleRepository scheduleRepository;

    private CalendarPK pk;

    @BeforeEach
    @Transactional
    public void seed() {
        YearMonth ym = YearMonth.of(2025, 8);
        pk = new CalendarPK(ym.getMonthValue(), ym.getYear());

        if (calendarRepository.findById(pk).isPresent()) {
            return;
        }


        Calendar cal = new Calendar(ym.getMonthValue(), ym.getYear());
        java.util.SortedSet<DayConfiguration> set = new java.util.TreeSet<DayConfiguration>();
        for (int d = 1; d <= 4; d++) {
            DayConfiguration dc = new DayConfiguration(d, true, 2, 0);
            dc.setCalendar(cal);
            set.add(dc);
        }
        cal.setDayConfigurations(set);
        calendarRepository.save(cal);


        Doctor d1 = doctorRepository.save(new Doctor("A","A","a@a.com", java.time.LocalDate.now()));
        Doctor d2 = doctorRepository.save(new Doctor("B","B","b@b.com", java.time.LocalDate.now()));

        ShiftConfiguration sc1 = new ShiftConfiguration(0, 10, 0, false, false);
        sc1.setDoctor(d1);
        shiftConfRepository.save(sc1);

        ShiftConfiguration sc2 = new ShiftConfiguration(0, 10, 0, false, false);
        sc2.setDoctor(d2);
        shiftConfRepository.save(sc2);


        List<DayConfiguration> dcs = dayConfigurationRepository
                .findByCalendarMonthAndCalendarYear(ym.getMonthValue(), ym.getYear());

        List<Shift> shifts = new ArrayList<Shift>();
        for (DayConfiguration dc : dcs) {
            Shift t1 = new Shift();
            t1.setDayConfiguration(dc);
            t1.setShiftType("TARDE");
            shifts.add(t1);

            Shift t2 = new Shift();
            t2.setDayConfiguration(dc);
            t2.setShiftType("TARDE");
            shifts.add(t2);

            if (dc.getDay() % 2 == 0) {
                Shift g = new Shift();
                g.setDayConfiguration(dc);
                g.setShiftType("GUARDIA");
                shifts.add(g);
            }
        }
        shiftRepository.saveAll(shifts);
    }

    @Test
    @Order(1)
    public void startScheduleGeneration_persistsSolutionAndScore() {
        Calendar cal = calendarRepository.findById(pk).orElseThrow(AssertionError::new);

        schedulerService.startScheduleGeneration(cal);

        Schedule persisted = scheduleRepository.findById(pk).orElseThrow(AssertionError::new);
        assertEquals(Schedule.ScheduleStatus.PENDING_CONFIRMATION, persisted.getStatus());
        assertNotNull(persisted.getShiftAssignments());
        assertFalse(persisted.getShiftAssignments().isEmpty());
        assertNotNull(persisted.getScore());
        HardSoftScore score = persisted.getScore();
        assertTrue(score.getHardScore() <= 0);
    }
}
