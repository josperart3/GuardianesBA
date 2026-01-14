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
import javax.validation.constraints.NotBlank;

/**
 * This class represents a specific work shift configuration
 * associated with a day, defining its type and requirements.
 * * @author josperart3
 */
@Entity
@Table(name = "shift",
    indexes = {
        @Index(name = "idx_shift_day_cfg", columnList = "dayconfiguration_day,dayconfiguration_calendar_month,dayconfiguration_calendar_year"),
        @Index(name = "idx_shift_day_type", columnList = "dayconfiguration_day,shift_type")
    })
public class Shift extends AbstractPersistable {


    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "dayconfiguration_day",            referencedColumnName = "day",            nullable = false),
        @JoinColumn(name = "dayconfiguration_calendar_month", referencedColumnName = "calendar_month", nullable = false),
        @JoinColumn(name = "dayconfiguration_calendar_year",  referencedColumnName = "calendar_year",  nullable = false)
    })
    private DayConfiguration dayConfiguration;

    @NotBlank
    @Column(name = "shift_type", nullable = false, length = 32)
    private String shiftType;

    @Column(name = "requires_skill", nullable = false)
    private boolean requiresSkill;

    @Column(name = "is_consultation", nullable = false)
    private boolean isConsultation;

    public Shift() {
    }

    public Shift(long id, DayConfiguration dayConfiguration, String shiftType) {
        super(id);
        this.dayConfiguration = dayConfiguration;
        this.shiftType = shiftType;
    }


    public DayConfiguration getDayConfiguration() {
        return dayConfiguration;
    }
    public void setDayConfiguration(DayConfiguration dayConfiguration) {
        this.dayConfiguration = dayConfiguration;
    }

    public String getShiftType() {
        return shiftType;
    }
    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    public boolean isConsultation() {
        return isConsultation;
    }
    public void setConsultation(boolean consultation) {
        isConsultation = consultation;
    }

    public boolean getRequiresSkill() {
        return requiresSkill;
    }
    public void setRequiresSkill(boolean requiresSkill) {
        this.requiresSkill = requiresSkill;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Shift)) return false;
        Shift other = (Shift) o;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public String toString() {
        return "Shift{" +
                "id=" + getId() +
                ", day=" + (dayConfiguration != null ? dayConfiguration.getDay() : "null") +
                ", type=" + shiftType +
                ", consultation=" + isConsultation +
                ", requiresSkill=" + requiresSkill +
                '}';
    }
}
