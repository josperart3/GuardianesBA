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
package us.dit.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import us.dit.service.model.entities.Calendar;
import us.dit.service.model.entities.*;
import us.dit.service.model.repositories.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Clase que usamos para cargar informacion inicial y poder trabajar con ella
 */
@Configuration
@Slf4j
@ConditionalOnProperty(name = "app.initdata.enable", havingValue = "true", matchIfMissing = false)
public class LoadInitialData {
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private ShiftConfigurationRepository shiftConfRepository;
    @Autowired
    private CalendarRepository calendarRepository;
    @Autowired
    private AllowedShiftRepository allowedShiftRepository;
    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Bean
    CommandLineRunner preloadInitialData() {
        return args -> {

            if (this.doctorRepository.findAll().isEmpty()
                    && this.shiftConfRepository.findAll().isEmpty()
                    && this.calendarRepository.findAll().isEmpty()
                    && this.allowedShiftRepository.findAll().isEmpty()
                    && this.rolRepository.findAll().isEmpty()) {
                log.info("Starting the preload of the initial data");


                LocalDate refDate = LocalDate.of(LocalDate.now().getYear(), LocalDate.now().getMonth().plus(1), 1);

                preloadRoles();
                // Preload the Doctors
                preloadDoctor(1, refDate, 0, 0, 0, true, false);
                preloadDoctor(2, refDate, 3, 5, 0, true, true);
                preloadDoctor(3, refDate.plusDays(1), 0, 0, 0, true, false);
                preloadDoctor(4, refDate.plusDays(1), 0, 0, 0, true, false);
                preloadDoctor(5, refDate.plusDays(2), 0, 0, 0, true, false);
                preloadDoctor(6, refDate.plusDays(2), 3, 5, 0, true, true);
                preloadDoctor(22, refDate.plusDays(10), 3, 5, 0, false, false);
                preloadDoctor(7, refDate.plusDays(3), 3, 5, 0, true, false);
                preloadDoctor(8, refDate.plusDays(3), 0, 0, 0, true, false);
                preloadDoctor(9, refDate.plusDays(4), 4, 5, 0, true, false);
                preloadDoctor(10, refDate.plusDays(4), 3, 5, 0, true, false);
                preloadDoctor(11, refDate.plusDays(5), 3, 5, 0, true, true);
                preloadDoctor(12, refDate.plusDays(5), 0, 0, 0, true, false);
                preloadDoctor(13, refDate.plusDays(6), 3, 5, 2, true, false);
                preloadDoctor(14, refDate.plusDays(6), 3, 5, 0, true, false);
                preloadDoctor(15, refDate.plusDays(7), 3, 5, 0, true, true);
                preloadDoctor(16, refDate.plusDays(7), 3, 5, 0, true, true);
                preloadDoctor(17, refDate.plusDays(8), 4, 5, 2, true, false);
                preloadDoctor(18, refDate.plusDays(8), 3, 5, 0, true, false);
                preloadDoctor(19, refDate.plusDays(9), 3, 5, 0, true, false);
                preloadDoctor(20, refDate.plusDays(9), 4, 5, 2, true, false);
                preloadDoctor(21, refDate.plusDays(10), 4, 5, 2, false, false);
                addAdminDoc();

                // Preload allowed shifts
                AllowedShift allowedShiftMonday = allowedShiftRepository.save(new AllowedShift("Monday"));
                log.info("Preloading " + allowedShiftMonday);
                AllowedShift allowedShiftTuesday = allowedShiftRepository.save(new AllowedShift("Tuesday"));
                log.info("Preloading " + allowedShiftTuesday);
                AllowedShift allowedShiftWednesday = allowedShiftRepository.save(new AllowedShift("Wednesday"));
                log.info("Preloading " + allowedShiftWednesday);
                AllowedShift allowedShiftThursday = allowedShiftRepository.save(new AllowedShift("Thursday"));
                log.info("Preloading " + allowedShiftThursday);
                AllowedShift allowedShiftFriday = allowedShiftRepository.save(new AllowedShift("Friday"));
                log.info("Preloading " + allowedShiftFriday);

                // To preload a calendar, use:
                preloadCalendarFor(YearMonth.of(2025, 8));
                executeSqlStatements();
                log.info("The preload of the initial data finished");
            } else {
                log.info("The preload of the initial data has been done before. It is not necessary to do it again.");
            }
        };
    }

    private void executeSqlStatements() {
        jdbcTemplate.execute("ALTER TABLE public.scheduleday_doctor ALTER COLUMN shifts_id DROP NOT NULL");
        jdbcTemplate.execute("ALTER TABLE public.scheduleday_doctor ALTER COLUMN cycle_id DROP NOT NULL");
        jdbcTemplate.execute("ALTER TABLE public.scheduleday_doctor ALTER COLUMN consultations_id DROP NOT NULL");
    }

    private void preloadRoles() {
        Rol doctor = new Rol("Doctor");
        Rol administrativo = new Rol("Administrativo");
        Rol administrador = new Rol("Administrador");
        rolRepository.save(doctor);
        rolRepository.save(administrador);
        rolRepository.save(administrativo);
        log.info("Roles creados");

    }

    @SuppressWarnings("unused")
    private void preloadCalendarFor(YearMonth yearMonth) {
        LocalDate day = null;
        Calendar calendar = new Calendar(yearMonth.getMonthValue(), yearMonth.getYear());
        DayConfiguration dayConf = null;
        SortedSet<DayConfiguration> dayConfs = new TreeSet<>();
        for (int i = 1; yearMonth.isValidDay(i); i++) {
            day = yearMonth.atDay(i);
            boolean isWorkingDay = day.getDayOfWeek() != DayOfWeek.SUNDAY && day.getDayOfWeek() != DayOfWeek.SATURDAY;
            dayConf = new DayConfiguration(i, isWorkingDay, 2, 0);
            dayConf.setCalendar(calendar);
            dayConfs.add(dayConf);
        }
        calendar.setDayConfigurations(dayConfs);
        calendar = calendarRepository.save(calendar);
        log.info("Preloading: " + calendar);
    }

    private void preloadDoctor(String name, String lastNames, String email, LocalDate startDate, int minShifts,
                               int maxShifts, int numConsultations, boolean doesCycleShifts, boolean hasShiftsOnlyWhenCycleShifts) {
        preloadDoctor(name, lastNames, email, startDate, minShifts, maxShifts, numConsultations, doesCycleShifts,
                hasShiftsOnlyWhenCycleShifts, new HashSet<>(), new HashSet<>(), new HashSet<>());
    }

    private void preloadDoctor(String name, String lastNames, String email, LocalDate startDate, int minShifts,
                               int maxShifts, int numConsultations, boolean doesCycleShifts, boolean hasShiftsOnlyWhenCycleShifts,
                               Set<AllowedShift> wantedShifts, Set<AllowedShift> unwantedShifts, Set<AllowedShift> wantedConsultations) {
        Doctor doctor = new Doctor(name, lastNames, email, startDate);
        Optional<Rol> roles = rolRepository.findBynombreRol("Doctor");
        doctor.addRole(roles.get());
        doctor = doctorRepository.save(doctor);
        log.info("Preloading: " + doctor);
        ShiftConfiguration shiftConf = new ShiftConfiguration(minShifts, maxShifts, numConsultations, doesCycleShifts,
                hasShiftsOnlyWhenCycleShifts);
        shiftConf.setDoctor(doctor);
        shiftConf.setWantedShifts(wantedShifts);
        shiftConf.setUnwantedShifts(unwantedShifts);
        shiftConf.setWantedConsultations(wantedConsultations);
        shiftConf = shiftConfRepository.save(shiftConf);
        log.info("Preloading: " + shiftConf);
    }

    private void preloadDoctor(int number, LocalDate startDate, int minShifts, int maxShifts, int numConsultations,
                               boolean doesCycleShifts, boolean hasShiftsOnlyWhenCycleShifts) {
        preloadDoctor(number + "", number + "", number + "@guardians.com", startDate, minShifts, maxShifts,
                numConsultations, doesCycleShifts, hasShiftsOnlyWhenCycleShifts);
    }

    private void addAdminDoc() {
        Doctor doctor = doctorRepository.findById((long) 20).get();
        Optional<Rol> roles = rolRepository.findBynombreRol("Administrador");
        doctor.addRole(roles.get());
        doctor = doctorRepository.save(doctor);
        Doctor doctor2 = doctorRepository.findById((long) 2).get();
        Optional<Rol> roles2 = rolRepository.findBynombreRol("Administrador");
        doctor2.addRole(roles2.get());
        doctor2 = doctorRepository.save(doctor2);
    }
}
