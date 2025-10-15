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
import lombok.EqualsAndHashCode;
import us.dit.service.model.validation.annotations.ValidAbsenceDates;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * This {@link Entity} represents the Absence of a {@link Doctor} during a
 * certain period An absence may occur, for example, if the {@link Doctor} is
 * sick or in their holidays
 * <p>
 * Absence is a weak entity. Hence, it receives its primary key from the
 * corresponding {@link Doctor}
 * <p>
 * There are certain constraints an Absence has to meet to be considered valid.
 * See {@link ValidAbsenceDates}
 *
 * @author miggoncan
 */
@Data
@EqualsAndHashCode(exclude = "doctor")
@Entity
@ValidAbsenceDates
public class Absence {
    /**
     * doctor_id is the primary key of the {@link Doctor} with this Absence
     */
    @Id
    private Long doctorId;
    @MapsId
    @OneToOne(optional = false)
    private Doctor doctor;

    /**
     * start is the day in which the Absence will begin
     */
    @Column(nullable = false)
    @NotNull
    private LocalDate startDate;

    /**
     * end is the day in which the Absence will finish
     */
    @Column(nullable = false)
    @NotNull
    private LocalDate endDate;

    public Absence(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Absence() {
    }
    
    public boolean isAbsentOn(LocalDate date) {
        if (date == null || startDate == null || endDate == null) return false;
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
        if (doctor != null) {
            this.doctorId = doctor.getId();
        } else {
            this.doctorId = null;
        }
    }

    // The toString method from @Data is not used as it can create an infinite loop
    // between Doctor#toString and this method
    @Override
    public String toString() {
        return Absence.class.getSimpleName() + "(" + "doctorId=" + this.doctorId + ", " + "start=" + this.startDate + ", "
                + "end=" + this.endDate + ")";
    }
}
