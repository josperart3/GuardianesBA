/**
*  This file is part of GuardianesBA - Business Application for processes managing healthcare tasks planning and supervision.
*  Copyright (C) 2026  Universidad de Sevilla/Departamento de Ingeniería Telemática
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

import javax.persistence.*;

import lombok.*;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.entity.PlanningPin;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
import org.optaplanner.core.api.domain.lookup.PlanningId;


/**
 * This class represents the assignment of a doctor to a shift
 * and acts as the planning entity for OptaPlanner.
 * * @author josperart3
 */
@Getter @Setter
@PlanningEntity
@Entity
@Table(name = "shift_assignment")
public class ShiftAssignment {

	@PlanningId
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    @ManyToOne(fetch = FetchType.LAZY)
    @PlanningVariable(valueRangeProviderRefs = {"doctorRange"}, nullable = true)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @PlanningPin
    @Column(name = "pinned", nullable = false)
    private boolean pinned = false;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "calendar_month", referencedColumnName = "calendar_month", nullable = false),
        @JoinColumn(name = "calendar_year",  referencedColumnName = "calendar_year",  nullable = false)
    })
    private Schedule schedule;

    public ShiftAssignment() { }
    public ShiftAssignment(Shift shift) { this.shift = shift; }


    public boolean isConsultation() { return shift != null && shift.isConsultation(); }
    public boolean requiresSkill() { return shift != null && shift.getRequiresSkill(); }
    public String getShiftType()   { return shift != null ? shift.getShiftType() : null; }
    public DayConfiguration getDayConfiguration() {
        return shift != null ? shift.getDayConfiguration() : null;
    }
    
    
    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }
    public Schedule getSchedule() {
        return schedule;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShiftAssignment)) return false;
        ShiftAssignment that = (ShiftAssignment) o;
        return id != null && id.equals(that.id);
    }
    @Override public int hashCode() { return 31; }
    @Override public String toString() {
    	return "ShiftAssignment{" +
                "id=" + id +
                ", shift=" + (shift != null ? shift.getId() : null) +
                ", doctor=" + (doctor != null ? (doctor.getFirstName() + " " + doctor.getLastNames()) : "null") +
                '}';
    }
}