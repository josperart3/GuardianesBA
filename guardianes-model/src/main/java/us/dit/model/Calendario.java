package us.dit.model;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Entity
public class Calendario {
	private static final Logger logger = LogManager.getLogger();

	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long idCalendario;

	@ElementCollection
	private Set<LocalDate> festivos;

	public Calendario(Set<LocalDate> festivos) {
		this.festivos = festivos;
		logger.info("Construyo sin el id el calendario");
	}

	public void agregarFestivo(int year, int month, int day) {
		LocalDate festivo = LocalDate.of(year, month, day);
		festivos.add(festivo);
	}
	
	public boolean esFestivo(LocalDate fecha) {
		return festivos.contains(fecha);
	}
	
	public void eliminarFestivo(LocalDate fecha) {
		festivos.remove(fecha);
	}
	
	public Set<LocalDate> getFestivos() {
		return festivos;
	}

	public void setFestivos(Set<LocalDate> festivos) { this.festivos = festivos;}

	public Long getIdCalendario() {
		return idCalendario;
	}

	@Override
	public String toString() {
		return super.toString()+" Calendario[festivos= "+festivos+"]\n";
	}
}
