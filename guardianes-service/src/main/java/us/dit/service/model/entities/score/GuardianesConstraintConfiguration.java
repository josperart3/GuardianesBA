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
package us.dit.service.model.entities.score;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.domain.constraintweight.ConstraintConfiguration;
import org.optaplanner.core.api.domain.constraintweight.ConstraintWeight;
import us.dit.service.model.entities.AbstractPersistable;


/**
 * This class defines the constraint configuration
 * that are used to manage weights and penalties for the scheduling rules required
 * by the OptaPlanner solver
 * 
 * @author josperart3
 */
@ConstraintConfiguration(constraintPackage = "us.dit.service.model.entities.score")
public class GuardianesConstraintConfiguration extends AbstractPersistable {

    // Reglas 
    public static final String EVERY_SHIFT_ASSIGNED = "Every shift must have a doctor";
    public static final String ELIGIBILITY_CYCLE = "Doctor eligibility for Guardias";
    public static final String HOLIDAYS = "Holidays for a doctor";
    public static final String INCOMPATIBLE_CONSULTA = "Incompatible: Consulta is exclusive";
    public static final String INCOMPATIBLE_DUPLICATES = "Incompatible: No duplicate types";
    public static final String CONDITIONAL_SHIFTS = "Linking: Shifts only when Cycle Shifts"; 
    public static final String DOCTOR_MAX_SHIFTS = "Doctor Max Shifts (Cont. Asist)";
    public static final String DOCTOR_SPECIFIC_CONSULTATIONS = "Strict Consultations Count";
    public static final String DOCTOR_MIN_SHIFTS_HARD = "Doctor Min Shifts Hard (Cont. Asist)";
    public static final String FAIRNESS_GUARDIAS = "Fairness in Guardias";
    public static final String MIN_DAYS_BETWEEN_GUARDIAS = "Minimum days between Guardias";
    public static final String AVOID_CONSECUTIVE_TARDES = "Avoid consecutive Tardes";

    // Hard Constraints 

    @ConstraintWeight(EVERY_SHIFT_ASSIGNED)
    private HardSoftScore everyShiftAssigned = HardSoftScore.ofHard(100);

    @ConstraintWeight(ELIGIBILITY_CYCLE)
    private HardSoftScore eligibilityCycle = HardSoftScore.ofHard(1000);

    @ConstraintWeight(HOLIDAYS)
    private HardSoftScore holidays = HardSoftScore.ofHard(1000);

    @ConstraintWeight(INCOMPATIBLE_CONSULTA)
    private HardSoftScore incompatibleConsulta = HardSoftScore.ofHard(1000);

    @ConstraintWeight(INCOMPATIBLE_DUPLICATES)
    private HardSoftScore incompatibleDuplicates = HardSoftScore.ofHard(1000);

    @ConstraintWeight(CONDITIONAL_SHIFTS)
    private HardSoftScore conditionalShifts = HardSoftScore.ofHard(1000);

    @ConstraintWeight(DOCTOR_MAX_SHIFTS)
    private HardSoftScore doctorMaxShifts = HardSoftScore.ofHard(100);

    @ConstraintWeight(DOCTOR_SPECIFIC_CONSULTATIONS)
    private HardSoftScore doctorSpecificConsultations = HardSoftScore.ofHard(100);
    
    @ConstraintWeight(DOCTOR_MIN_SHIFTS_HARD)
    private HardSoftScore doctorMinShiftsHard = HardSoftScore.ofHard(100);

    // Soft Constraints

    @ConstraintWeight(FAIRNESS_GUARDIAS)
    private HardSoftScore fairnessGuardias = HardSoftScore.ofSoft(10);

    @ConstraintWeight(MIN_DAYS_BETWEEN_GUARDIAS)
    private HardSoftScore minDaysBetweenGuardias = HardSoftScore.ofSoft(50);
    
    @ConstraintWeight(AVOID_CONSECUTIVE_TARDES)
    private HardSoftScore avoidConsecutiveTardes = HardSoftScore.ofSoft(10);


    public GuardianesConstraintConfiguration() {
    }

    public GuardianesConstraintConfiguration(long id) {
        super(id);
    }

    // ========================================================================
    // GETTERS Y SETTERS
    // ========================================================================

    public HardSoftScore getEveryShiftAssigned(){
        return everyShiftAssigned; 
    }
    
    public void setEveryShiftAssigned(HardSoftScore s){
        this.everyShiftAssigned = s; 
    }

    public HardSoftScore getEligibilityCycle(){
        return eligibilityCycle;
    }
    
    public void setEligibilityCycle(HardSoftScore s){
        this.eligibilityCycle = s; 
    }

    public HardSoftScore getHolidays(){
        return holidays;
    }
    
    public void setHolidays(HardSoftScore s){
        this.holidays = s;
    }

    public HardSoftScore getIncompatibleConsulta(){
        return incompatibleConsulta;
    }
    
    public void setIncompatibleConsulta(HardSoftScore s){
        this.incompatibleConsulta = s;
    }

    public HardSoftScore getIncompatibleDuplicates(){
        return incompatibleDuplicates;
    }
    
    public void setIncompatibleDuplicates(HardSoftScore s){
        this.incompatibleDuplicates = s;
    }

    public HardSoftScore getConditionalShifts(){
        return conditionalShifts;
    }
    
    public void setConditionalShifts(HardSoftScore s){ 
        this.conditionalShifts = s;
    }

    public HardSoftScore getDoctorMaxShifts(){ 
        return doctorMaxShifts; 
    }
    
    public void setDoctorMaxShifts(HardSoftScore s){ 
        this.doctorMaxShifts = s; 
    }

    public void setDoctorSpecificConsultations(HardSoftScore s){ 
        this.doctorSpecificConsultations = s; 
    }
    
    public HardSoftScore getDoctorMinShiftsHard(){ 
        return doctorMinShiftsHard; 
    }
    
    public void setDoctorMinShiftsHard(HardSoftScore s){ 
        this.doctorMinShiftsHard = s; 
    }
    
    public HardSoftScore getDoctorSpecificConsultations(){ 
        return doctorSpecificConsultations; 
    }
    
    public HardSoftScore getFairnessGuardias(){ 
        return fairnessGuardias; 
    }
    
    public void setFairnessGuardias(HardSoftScore s){ 
        this.fairnessGuardias = s; 
    }

    public HardSoftScore getMinDaysBetweenGuardias(){ 
        return minDaysBetweenGuardias; 
    }
    
    public void setMinDaysBetweenGuardias(HardSoftScore s){ 
        this.minDaysBetweenGuardias = s; 
    }

    public HardSoftScore getAvoidConsecutiveTardes(){ 
        return avoidConsecutiveTardes; 
    }
    
    public void setAvoidConsecutiveTardes(HardSoftScore s){ 
        this.avoidConsecutiveTardes = s; 
    }
}