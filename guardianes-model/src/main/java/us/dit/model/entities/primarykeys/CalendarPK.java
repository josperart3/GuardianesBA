package us.dit.model.entities.primarykeys;

import lombok.Data;
import us.dit.model.entities.Calendar;

import javax.persistence.Entity;
import java.io.Serializable;

/**
 * This class represents the primary key for the {@link Entity} {@link Calendar}
 * <p>
 * A {@link Calendar} is uniquely identified with a month and a year
 *
 * @author miggoncan
 */
@Data
public class CalendarPK implements Serializable {
    private static final long serialVersionUID = 66688158711309197L;

    private Integer month;
    private Integer year;

    public CalendarPK(Integer month, Integer year) {
        this.month = month;
        this.year = year;
    }

    public CalendarPK() {
    }
}