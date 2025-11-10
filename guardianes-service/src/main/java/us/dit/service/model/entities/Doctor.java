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
import lombok.ToString;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * This {@link Entity} represents the information of a Doctor that will be
 * stored in the database
 * <p>
 * {@link Doctor}s have some periodic shifts. This is, if some {@link Doctor}s
 * have a shift today, after a certain number of days, they will have another
 * one. This kind of shifts will be referred to as "cycle-shift", and should not
 * be confused with regular shifts. A "regular shift", or "shift" in short,
 * refers to the shifts that will vary from month to month and that do not occur
 * periodically. This kind of shifts are scheduled from {@link Doctor#startDate}
 *
 * @author miggoncan
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Entity
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    @Column(name = "id")
    private Long id;
    
    @Column(name = "TelegramID")
    private String telegramId;
    
    @Column(nullable = false)
    @NotBlank
    private String firstName;
    
    @Column(nullable = false)
    @NotBlank
    private String lastNames;
    
    @Email
    @NotBlank
    @Column(unique = true, nullable = false)
    private String email;
    
    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private DoctorStatus status = DoctorStatus.AVAILABLE;
    
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToOne(optional = true, mappedBy = "doctor", cascade = {CascadeType.ALL})
    private Absence absence;
    
    @Column(nullable = false)
    private LocalDate startDate;
    
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "doctor_roles",
            joinColumns = @JoinColumn(name = "doctor_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    private Set<Rol> roles = new HashSet<>();

    public Doctor(String firsName, String lastNames, String email, LocalDate startDate) {
        this.firstName = firsName;
        this.lastNames = lastNames;
        this.email = email;
        this.startDate = startDate;
    }

    public Doctor() {
    }


    @Override
    public String toString() {
        return Doctor.class.getSimpleName()
                + "("
                + "id=" + this.id + ", "
                + "firstName=" + this.firstName + ", "
                + "lastNames=" + this.lastNames + ", "
                + "email=" + this.email + ", "
                + "status=" + this.status + ", "
                + "absence=" + this.absence + ", "
                + "startDate=" + this.startDate
                + ")";
    }

    public void setAbsence(Absence absence) {
        this.absence = absence;
        if (absence != null) {
            this.absence.setDoctor(this);
        }
    }

    public void addRole(Rol role) {
        this.roles.add(role);
    }

    public String getTelegramId() {
        return this.telegramId;
    }

    public void setTelegramId(String telID) {
        this.telegramId = telID;
    }

    public Rol getRol(String nombre) {
        for (Rol element : roles) {
            if (element.getNombreRol().equals(nombre))
                return element;

        }
        return null;

    }

    public ShiftConfiguration getShiftConfiguration() {
        return shiftConfiguration;
    }
    
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToOne(mappedBy = "doctor", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    private ShiftConfiguration shiftConfiguration;
    
    public void setShiftConfiguration(ShiftConfiguration sc) {
        this.shiftConfiguration = sc;
        if (sc != null && sc.getDoctor() != this) {
            sc.setDoctor(this);
        }
    }
    
    public boolean hasRequiredSkill() {
        return true;
    }

    public enum DoctorStatus {
        AVAILABLE,
        DELETED
    }
}
