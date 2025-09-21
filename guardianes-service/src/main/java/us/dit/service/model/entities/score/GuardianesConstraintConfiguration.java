package us.dit.service.model.entities.score;


import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.domain.constraintweight.ConstraintConfiguration;
import org.optaplanner.core.api.domain.constraintweight.ConstraintWeight;
import us.dit.service.model.entities.AbstractPersistable;

@ConstraintConfiguration(constraintPackage = "us.dit.service.model.entities.score")
public class GuardianesConstraintConfiguration extends AbstractPersistable {

    public static final String MAX_SHIFTS = "Maximun number of shifts in the afternoon for a doctor";
    public static final String MIN_SHIFTS = "Minimun number of shifts in the afternoon for a doctor";
    public static final String NUM_CONSULTATIONS = "Number of consultations a doctor do";
    public static final String DOES_CYCLE_SHIFTS = "A doctor does night guards";
    public static final String HAS_SHIFTS_ONLY_WHEN_CYCLE_SHIFTS = "A doctor does afternoon shifts when they do night guards";
    public static final String UNWANTED_SHIFTS = "Unwanted afternoon shifts for a doctor during a week";
    public static final String UNAVAILABLE_SHIFTS = "Unavailable afternoon shifts for a doctor during a week";
    public static final String WANTED_SHIFTS = "Wanted afternoon shifts for a doctor during a week";
    public static final String MANDATORY_SHIFTS = "Mandatory afternoon shifts for a doctor during a week";
    public static final String WANTED_CONSULTATIONS = "Wanted consultations for a doctor during a week";
    public static final String IS_WORKING_DAY = "Working day";
    public static final String NUM_SHIFTS = "Number of afternoon shifts that should be in a day of the week";
    public static final String NUM_CONSULTATIONS_IN_A_DAY = "Number of consultations in a day of the week";
    public static final String HOLIDAYS = "Holidays for a doctor";
    public static final String SKILL_REQUIRED = "Skill required for a doctor to work in this station";
    public static final String MIN_DAYS_BETWEEN_CYCLE_SHIFTS = "Minimum number of days between night guards fot a doctor";

    // HARD constraints
    @ConstraintWeight(MAX_SHIFTS)
    private HardSoftScore maxShifts = HardSoftScore.ofHard(1000);

    @ConstraintWeight(MIN_SHIFTS)
    private HardSoftScore minShifts = HardSoftScore.ofHard(1000);

    @ConstraintWeight(DOES_CYCLE_SHIFTS)
    private HardSoftScore doesCycleShifts = HardSoftScore.ofHard(100000);

    @ConstraintWeight(HAS_SHIFTS_ONLY_WHEN_CYCLE_SHIFTS)
    private HardSoftScore hasShiftsOnlyWhenCycleShifts = HardSoftScore.ofHard(1);

    @ConstraintWeight(UNAVAILABLE_SHIFTS)
    private HardSoftScore unavailableShifts = HardSoftScore.ofHard(10000);

    @ConstraintWeight(MANDATORY_SHIFTS)
    private HardSoftScore mandatoryShifts = HardSoftScore.ofHard(10000);

    @ConstraintWeight(IS_WORKING_DAY)
    private HardSoftScore isWorkingDay = HardSoftScore.ofHard(100000);

    @ConstraintWeight(NUM_SHIFTS)
    private HardSoftScore numShifts = HardSoftScore.ofHard(1);

    @ConstraintWeight(NUM_CONSULTATIONS_IN_A_DAY)
    private HardSoftScore numConsultationsInDay = HardSoftScore.ofHard(1);

    @ConstraintWeight(HOLIDAYS)
    private HardSoftScore holidays = HardSoftScore.ofHard(100000);

    @ConstraintWeight(MIN_DAYS_BETWEEN_CYCLE_SHIFTS)
    private HardSoftScore minDaysBetweenCycleShifts = HardSoftScore.ofHard(100000);

    @ConstraintWeight(SKILL_REQUIRED)
    private HardSoftScore skillRequired = HardSoftScore.ofHard(100);

    // SOFT constraints
    @ConstraintWeight(NUM_CONSULTATIONS)
    private HardSoftScore num_consultations = HardSoftScore.ofSoft(1);

    @ConstraintWeight(UNWANTED_SHIFTS)
    private HardSoftScore unwantedShifts = HardSoftScore.ofSoft(10);

    @ConstraintWeight(WANTED_SHIFTS)
    private HardSoftScore wantedShifts = HardSoftScore.ofSoft(10);

    @ConstraintWeight(WANTED_CONSULTATIONS)
    private HardSoftScore wantedConsultations = HardSoftScore.ofSoft(10);

    public GuardianesConstraintConfiguration() {
    }

    public GuardianesConstraintConfiguration(long id) {
        super(id);
    }

    public HardSoftScore getMaxShifts() {
        return maxShifts;
    }

    public void setMaxShifts(HardSoftScore maxShifts) {
        this.maxShifts = maxShifts;
    }

    public HardSoftScore getMinShifts() {
        return minShifts;
    }

    public void setMinShifts(HardSoftScore minShifts) {
        this.minShifts = minShifts;
    }

    public HardSoftScore getDoesCycleShifts() {
        return doesCycleShifts;
    }

    public void setDoesCycleShifts(HardSoftScore doesCycleShifts) {
        this.doesCycleShifts = doesCycleShifts;
    }

    public HardSoftScore getHasShiftsOnlyWhenCycleShifts() {
        return hasShiftsOnlyWhenCycleShifts;
    }

    public void setHasShiftsOnlyWhenCycleShifts(HardSoftScore hasShiftsOnlyWhenCycleShifts) {
        this.hasShiftsOnlyWhenCycleShifts = hasShiftsOnlyWhenCycleShifts;
    }

    public HardSoftScore getUnavailableShifts() {
        return unavailableShifts;
    }

    public void setUnavailableShifts(HardSoftScore unavailableShifts) {
        this.unavailableShifts = unavailableShifts;
    }

    public HardSoftScore getMandatoryShifts() {
        return mandatoryShifts;
    }

    public void setMandatoryShifts(HardSoftScore mandatoryShifts) {
        this.mandatoryShifts = mandatoryShifts;
    }

    public HardSoftScore getIsWorkingDay() {
        return isWorkingDay;
    }

    public void setIsWorkingDay(HardSoftScore isWorkingDay) {
        this.isWorkingDay = isWorkingDay;
    }

    public HardSoftScore getNumShifts() {
        return numShifts;
    }

    public void setNumShifts(HardSoftScore numShifts) {
        this.numShifts = numShifts;
    }

    public HardSoftScore getNumConsultationsInDay() {
        return numConsultationsInDay;
    }

    public void setNumConsultationsInDay(HardSoftScore numConsultationsInDay) {
        this.numConsultationsInDay = numConsultationsInDay;
    }

    public HardSoftScore getHolidays() {
        return holidays;
    }

    public void setHolidays(HardSoftScore holidays) {
        this.holidays = holidays;
    }

    public HardSoftScore getMinDaysBetweenCycleShifts() {
        return minDaysBetweenCycleShifts;
    }

    public void setMinDaysBetweenCycleShifts(HardSoftScore minDaysBetweenCycleShifts) {
        this.minDaysBetweenCycleShifts = minDaysBetweenCycleShifts;
    }

    public HardSoftScore getSkillRequired() {
        return skillRequired;
    }

    public void setSkillRequired(HardSoftScore skillRequired) {
        this.skillRequired = skillRequired;
    }

    public HardSoftScore getNum_consultations() {
        return num_consultations;
    }

    public void setNum_consultations(HardSoftScore num_consultations) {
        this.num_consultations = num_consultations;
    }

    public HardSoftScore getUnwantedShifts() {
        return unwantedShifts;
    }

    public void setUnwantedShifts(HardSoftScore unwantedShifts) {
        this.unwantedShifts = unwantedShifts;
    }

    public HardSoftScore getWantedShifts() {
        return wantedShifts;
    }

    public void setWantedShifts(HardSoftScore wantedShifts) {
        this.wantedShifts = wantedShifts;
    }

    public HardSoftScore getWantedConsultations() {
        return wantedConsultations;
    }

    public void setWantedConsultations(HardSoftScore wantedConsultations) {
        this.wantedConsultations = wantedConsultations;
    }
}
