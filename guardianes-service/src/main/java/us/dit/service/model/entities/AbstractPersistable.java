package us.dit.service.model.entities;


import org.optaplanner.core.api.domain.lookup.PlanningId;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;

@MappedSuperclass
public abstract class AbstractPersistable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@PlanningId
    protected Long id;

    protected AbstractPersistable() {
    }

    protected AbstractPersistable(Long id) {
        this.id = id;
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return getClass().getName().replaceAll(".*\\.", "") + "-" + id;
    }

}
