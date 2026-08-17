package it.fin8.gdrsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.fin8.gdrsheet.def.TipoCatalogoIncantesimo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Abilitazione opt-in di un valore {@link CatalogoIncantesimo} per un mondo: stessa logica di
 * {@link MondoListaIncantesimiAbilitata}, per le 4 liste di corredo di un incantesimo (Scuola/
 * Sottoscuola/Descrittore/Componente) invece delle liste/domìni SP_*. Il valore è duplicato qui
 * (non un FK a CatalogoIncantesimo) per semplicità: nessuna validazione referenziale, solo un
 * elenco di stringhe abilitate per (mondo, tipo).
 */
@Getter
@Setter
@Entity
@Table(name = "mondo_catalogo_incantesimo_abilitato")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MondoCatalogoIncantesimoAbilitato implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_mondo", nullable = false)
    private Mondo mondo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoCatalogoIncantesimo tipo;

    @NotBlank
    @Size(max = 100)
    @Column(name = "valore", nullable = false, length = 100)
    private String valore;

}
