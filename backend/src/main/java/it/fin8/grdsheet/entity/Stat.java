package it.fin8.grdsheet.entity;

import it.fin8.grdsheet.def.TipoStat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "stats")
public class Stat {
    @Id
    @Size(max = 10)
    @Column(name = "id", nullable = false, length = 10)
    private String id;

    @Size(max = 64)
    @NotNull
    @Column(name = "label", nullable = false, length = 64)
    private String label;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_mod")
    private Stat defaultMod;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoStat tipo;

}