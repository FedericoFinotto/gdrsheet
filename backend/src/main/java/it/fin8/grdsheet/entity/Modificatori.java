package it.fin8.grdsheet.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "modificatori")
public class Modificatori {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_item", nullable = false)
    private Item idItem;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_stat", nullable = false)
    private TipoStat idStat;

    @NotNull
    @Column(name = "valore", nullable = false)
    private Integer valore;

    @Column(name = "nota", length = Integer.MAX_VALUE)
    private String nota;

}