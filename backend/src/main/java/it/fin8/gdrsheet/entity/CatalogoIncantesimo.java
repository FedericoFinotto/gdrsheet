package it.fin8.gdrsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.fin8.gdrsheet.def.TipoCatalogoIncantesimo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Catalogo globale, condiviso tra tutti i mondi, di un valore per una delle 4 liste "di corredo"
 * di un incantesimo (Scuola/Sottoscuola/Descrittore/Componente) — stessa logica di
 * {@link ListaIncantesimi}, ma qui il valore stesso è già l'etichetta mostrata (nessuna etichetta
 * separata). Ciò che varia per mondo è quali valori sono abilitati, vedi
 * {@link MondoCatalogoIncantesimoAbilitato}.
 */
@Getter
@Setter
@Entity
@Table(name = "catalogo_incantesimo", uniqueConstraints = @UniqueConstraint(columnNames = {"tipo", "valore"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CatalogoIncantesimo implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoCatalogoIncantesimo tipo;

    @NotBlank
    @Size(max = 100)
    @Column(name = "valore", nullable = false, length = 100)
    private String valore;

}
