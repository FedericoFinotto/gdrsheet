package it.fin8.grdsheet.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "default_stat")
public class DefaultStat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_mondo", nullable = false)
    private Mondo idMondo;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sistema", nullable = false)
    private Sistema idSistema;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_stat_id", nullable = false)
    private TipoStat tipoStat;

    @Size(max = 32)
    @Column(name = "label", length = 32)
    private String label;

    @Column(name = "valore_default")
    private Integer valoreDefault;

}