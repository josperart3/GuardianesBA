package us.dit.service.model.entities;

import javax.persistence.*;

import lombok.*;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.entity.PlanningPin;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

@Getter @Setter
@PlanningEntity
@Entity
@Table(name = "shift_assignment")
public class ShiftAssignment {

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