package us.dit.service.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import us.dit.service.model.entities.Shift;
import java.util.List;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {
    List<Shift> findByDayConfigurationCalendarMonthAndDayConfigurationCalendarYear(Integer month, Integer year);
}
