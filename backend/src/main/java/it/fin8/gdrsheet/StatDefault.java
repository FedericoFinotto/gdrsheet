package it.fin8.gdrsheet;

import it.fin8.gdrsheet.entity.Mondo;
import it.fin8.gdrsheet.entity.Sistema;
import it.fin8.gdrsheet.entity.Stat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "stat_default")
public class StatDefault {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_mondo")
    private Mondo mondo;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sistema", nullable = false)
    private Sistema sistema;

    @NotNull
    @Column(name = "stat_id", nullable = false, length = Integer.MAX_VALUE)
    private String statId;

    @Column(name = "valore_default", length = Integer.MAX_VALUE)
    private String valoreDefault;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_mod")
    private Stat defaultMod;

    @Column(name = "addestramento")
    private Boolean addestramento;

}