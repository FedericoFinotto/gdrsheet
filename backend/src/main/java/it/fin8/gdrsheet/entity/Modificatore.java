package it.fin8.gdrsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.fin8.gdrsheet.def.TipoModificatore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "modificatori")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "item"})
public class Modificatore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_item", nullable = false)
    private Item item;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_stat", nullable = false)
    private Stat stat;

    @NotNull
    @Column(name = "valore", nullable = false)
    private String valore;

    @Column(name = "nota", length = Integer.MAX_VALUE)
    private String nota;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoModificatore tipo;

    @Column(name = "always", length = Integer.MAX_VALUE)
    private Boolean sempreAttivo;

    /**
     * Per le copie create da un grant di classe: id del modificatore sorgente.
     * Null per i modificatori "propri" del livello (BASE, RANK e quelli liberi).
     */
    @Column(name = "id_sorgente")
    private Integer idSorgente;

    /**
     * Marca i modificatori creati da un campo dedicato dell'editor (un "template fisso"),
     * invece che liberamente dall'utente — es. {@link it.fin8.gdrsheet.config.Constants#PLACEHOLDER_LIVELLO_PUNTI_FERITA}.
     * A differenza di idSorgente, identifica in modo stabile UNA riga specifica anche se ne
     * esistono altre simili (stessa stat, nessuna nota) aggiunte a mano dall'utente.
     */
    @Column(name = "placeholder")
    private String placeholder;

}