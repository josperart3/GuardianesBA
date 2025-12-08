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
package us.dit.service.model.entities;

import lombok.Data;

import org.hibernate.annotations.SortNatural;
import org.hibernate.validator.constraints.Range;
import us.dit.service.model.entities.primarykeys.CalendarPK;
import us.dit.service.model.validation.annotations.ValidSchedule;

import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.ProblemFactProperty;
import org.optaplanner.core.api.domain.lookup.LookUpStrategyType;

import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

import org.optaplanner.persistence.jpa.api.score.buildin.hardsoft.HardSoftScoreConverter;
import org.optaplanner.core.api.domain.constraintweight.ConstraintConfigurationProvider;

import javax.persistence.*;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import org.springframework.data.domain.Persistable;

/**
 * The Schedule {@link Entity} represents the scheduled shifts of a specific
 * {@link Calendar}
 * <p>
 * Note the primary key of this entity is composite, hence the {@link IdClass}
 * annotation. Moreover, this is a weak entity, so it receives its primary key
 * from the {@link Calendar} it is associated to
 *
 * @author miggoncan
 * @see ScheduleDay
 */
@Data
@Entity
@Table(name = "schedule")
@IdClass(CalendarPK.class) // <<--- usamos IdClass, NO EmbeddedId
@PlanningSolution(lookUpStrategyType = LookUpStrategyType.PLANNING_ID_OR_NONE)
public class Schedule implements Persistable<CalendarPK>{


    @Id
    @Column(name = "calendar_month", nullable = false)
    @Min(1) @Max(12) @NotNull
    private Integer month;

    @Id
    @Column(name = "calendar_year", nullable = false)
    @Min(1970) @NotNull
    private Integer year;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
        @JoinColumn(name = "calendar_month", referencedColumnName = "month", insertable = false, updatable = false),
        @JoinColumn(name = "calendar_year",  referencedColumnName = "year",  insertable = false, updatable = false)
    })
    private Calendar calendar;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(name = "status", nullable = false)
    private ScheduleStatus status = ScheduleStatus.NOT_CREATED;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @SortNatural
    private SortedSet<ScheduleDay> days;

    @Transient
    private boolean isNew = true;

    @Override
    public CalendarPK getId() {
        return new CalendarPK(month, year);
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    // Método para cambiar el estado después de guardar si fuera necesario
    public void setIsNew(boolean isNew) {
        this.isNew = isNew;
    }

    // ---- Entidades planificables ----
    @PlanningEntityCollectionProperty
    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShiftAssignment> shiftAssignments = new ArrayList<>();

    // ---- Hechos / rangos ----
    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "doctorRange")
    @ManyToMany
    @JoinTable(
        name = "schedule_doctors",
        joinColumns = {
            @JoinColumn(name = "sched_calendar_month", referencedColumnName = "calendar_month"),
            @JoinColumn(name = "sched_calendar_year",  referencedColumnName = "calendar_year")
        },
        inverseJoinColumns = @JoinColumn(name = "doctor_id", referencedColumnName = "id")
    )
    private List<Doctor> doctorList = new ArrayList<>();

    @ProblemFactCollectionProperty
    @ManyToMany
    @JoinTable(
        name = "schedule_shifts",
        joinColumns = {
            @JoinColumn(name = "sched_calendar_month", referencedColumnName = "calendar_month"),
            @JoinColumn(name = "sched_calendar_year",  referencedColumnName = "calendar_year")
        },
        inverseJoinColumns = @JoinColumn(name = "shift_id", referencedColumnName = "id")
    )
    private List<Shift> shiftList = new ArrayList<>();

    @ProblemFactCollectionProperty
    @ManyToMany
    @JoinTable(
        name = "schedule_day_cfg",
        joinColumns = {
            @JoinColumn(name = "sched_calendar_month", referencedColumnName = "calendar_month"),
            @JoinColumn(name = "sched_calendar_year",  referencedColumnName = "calendar_year")
        },
        inverseJoinColumns = {
            @JoinColumn(name = "dc_day",            referencedColumnName = "day"),
            @JoinColumn(name = "dc_calendar_month", referencedColumnName = "calendar_month"),
            @JoinColumn(name = "dc_calendar_year",  referencedColumnName = "calendar_year")
        }
    )
    private List<DayConfiguration> dayConfigurationList = new ArrayList<>();

    // ---- Constraint configuration ---- 
    @ConstraintConfigurationProvider
    @Transient
    private us.dit.service.model.entities.score.GuardianesConstraintConfiguration constraintConfiguration
            = new us.dit.service.model.entities.score.GuardianesConstraintConfiguration(0L);

    @PlanningScore
    @Convert(converter = HardSoftScoreConverter.class)
    @Column(name = "score")
    private HardSoftScore score;

    public Schedule() {}
    public Schedule(ScheduleStatus status) { this.status = status; }

    public void setId(CalendarPK pk) {
        if (pk != null) {
            this.month = pk.getMonth();
            this.year = pk.getYear();
        }
    }
    
    public enum ScheduleStatus {
        NOT_CREATED, BEING_GENERATED, PENDING_CONFIRMATION, CONFIRMED, GENERATION_ERROR
    }
}