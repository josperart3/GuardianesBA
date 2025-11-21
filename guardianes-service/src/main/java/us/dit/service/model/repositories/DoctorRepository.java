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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import us.dit.service.model.entities.Doctor;

import javax.persistence.Entity;
import java.util.List;
import java.util.Optional;


/**
 * This interface will be used by Jpa to auto-generate a class having all the
 * CRUD operations on the {@link Doctor} {@link Entity}. This operations will be
 * performed differently depending on the configured data-source. But this is
 * completely transparent to the application
 *
 * @author miggoncan
 */
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    /**
     * Retrieve a {@link Doctor} from the database if it exists, provided its
     * email
     *
     * @param email The email of the {@link Doctor}
     * @return The {@link Doctor}, if found
     */
    public Optional<Doctor> findByEmail(String email);

    /**
     * Retrieve a {@link Doctor} from the database if it exists, provided its
     * Telegram ID
     *
     * @param telegramId
     * @return
     * @author carcohcal
     */
    public Optional<Doctor> findBytelegramId(String telegramId);


    @Query("SELECT u FROM Doctor u JOIN u.roles r WHERE r.nombreRol LIKE %?1%")
    public List<Doctor> findByRol(String keyword);
}
