package it.fin8.grdsheet.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "stat_value")
public class StatValue {
    @Id
    @ColumnDefault("nextval('stat_value_id_seq')")
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "personaggio_id", nullable = false)
    private Personaggio personaggio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_stat_id", nullable = false)
    private TipoStat tipoStat;

    @Column(name = "valore", nullable = false)
    private Integer valore;

}