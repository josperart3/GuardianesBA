package us.dit.service.model.entities;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;


/**
 * Turno/día concreto. Entidad JPA usada como Problem Fact en Schedule.
 */
@Entity
@Table(name = "shift",
       indexes = {
         @Index(name = "idx_shift_day_cfg", columnList = "dayconfiguration_day,dayconfiguration_calendar_month,dayconfiguration_calendar_year"),
         @Index(name = "idx_shift_day_type", columnList = "dayconfiguration_day,shift_type")
       })
public class Shift extends AbstractPersistable {

    /** Día al que pertenece el turno (PK compuesta en DayConfiguration). */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "dayconfiguration_day",            referencedColumnName = "day",            nullable = false),
        @JoinColumn(name = "dayconfiguration_calendar_month", referencedColumnName = "calendar_month", nullable = false),
        @JoinColumn(name = "dayconfiguration_calendar_year",  referencedColumnName = "calendar_year",  nullable = false)
    })
    private DayConfiguration dayConfiguration;

    /** Tipo de turno: p.ej. "TARDE", "GUARDIA"… (lo usan las reglas DRL). */
    @NotBlank
    @Column(name = "shift_type", nullable = false, length = 32)
    private String shiftType;

    /** Si el puesto requiere una skill concreta (lo usan las reglas DRL). */
    @Column(name = "requires_skill", nullable = false)
    private boolean requiresSkill;

    /** Si este turno es una consulta (lo usan las reglas DRL). */
    @Column(name = "is_consultation", nullable = false)
    private boolean isConsultation;

    public Shift() {
    }

    public Shift(long id, DayConfiguration dayConfiguration, String shiftType) {
        super(id);
        this.dayConfiguration = dayConfiguration;
        this.shiftType = shiftType;
    }

    // --- Getters/Setters ---

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

    // Importante: mantener el nombre "isConsultation()" para que en DRL puedas usar "consultation"
    public boolean isConsultation() {
        return isConsultation;
    }
    public void setConsultation(boolean consultation) {
        isConsultation = consultation;
    }

    // En tu DRL usas shift.requiresSkill, así que este getter debe llamarse "getRequiresSkill()"
    public boolean getRequiresSkill() {
        return requiresSkill;
    }
    public void setRequiresSkill(boolean requiresSkill) {
        this.requiresSkill = requiresSkill;
    }

    // --- equals/hashCode por id (patrón JPA seguro) ---

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
