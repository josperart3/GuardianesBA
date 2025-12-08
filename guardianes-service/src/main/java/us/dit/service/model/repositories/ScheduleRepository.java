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
package us.dit.service.model.repositories;

import us.dit.service.model.entities.Schedule;
import us.dit.service.model.entities.primarykeys.CalendarPK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.Entity;
import java.util.Optional;

/**
 * This interface will be used by Jpa to auto-generate a class having all the
 * CRUD operations on the {@link Schedule} {@link Entity}. This operations will
 * be performed differently depending on the configured data-source. But this is
 * completely transparent to the application
 * 
 * @author miggoncan
 */
@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, CalendarPK> {

    // 1. Consulta base: Trae el Schedule y la lista de días (ScheduleDay)
    // Esto inicializa la colección 'days', pero las listas internas de cada día siguen vacías.
    @Query("SELECT DISTINCT s FROM Schedule s LEFT JOIN FETCH s.days WHERE s.month = :month AND s.year = :year")
    Optional<Schedule> findByIdWithDays(@Param("month") Integer month, @Param("year") Integer year);

    // 2. Consulta auxiliar: Rellena los turnos (shifts) de los días ya cargados en memoria
    @Query("SELECT DISTINCT s FROM Schedule s LEFT JOIN FETCH s.days d LEFT JOIN FETCH d.shifts WHERE s.month = :month AND s.year = :year")
    Schedule fetchShifts(@Param("month") Integer month, @Param("year") Integer year);

    // 3. Consulta auxiliar: Rellena los ciclos (cycle) de los días ya cargados
    @Query("SELECT DISTINCT s FROM Schedule s LEFT JOIN FETCH s.days d LEFT JOIN FETCH d.cycle WHERE s.month = :month AND s.year = :year")
    Schedule fetchCycle(@Param("month") Integer month, @Param("year") Integer year);

    // 4. Consulta auxiliar: Rellena las consultas (consultations) de los días ya cargados
    @Query("SELECT DISTINCT s FROM Schedule s LEFT JOIN FETCH s.days d LEFT JOIN FETCH d.consultations WHERE s.month = :month AND s.year = :year")
    Schedule fetchConsultations(@Param("month") Integer month, @Param("year") Integer year);
}
