package it.fin8.gdrsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.fin8.gdrsheet.def.TipoStat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "stats")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Stat {
	@Id
	@Size(max = 10)
	@Column(name = "id", nullable = false, length = 10)
	private String id;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false)
	private TipoStat tipo;

	@Size(max = 64)
	@NotNull
	@Column(name = "label", nullable = false, length = 64)
	private String label;

	/** Se false, l'abilità non compare in Gestisci Gradi né nella pagina Livelli (non "sale" con
	 *  i livelli), ma resta visibile in scheda e utilizzabile come bersaglio di Modificatore. */
	@NotNull
	@Column(name = "rankable", nullable = false)
	private Boolean rankable = true;

}