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

}