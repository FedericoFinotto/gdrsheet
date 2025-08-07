package it.fin8.gdrsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "stat_value")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StatValue implements Serializable {
    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "personaggio_id", nullable = false)
    private Personaggio personaggio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stat_id", nullable = false)
    private Stat stat;

    @Column(name = "valore", nullable = false)
    private String valore;

    @Column(name = "formula", nullable = true)
    private String formula;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "mod", nullable = true)
    private Stat mod;

    @Column(name = "classe", nullable = false)
    private Boolean classe;

    @Column(name = "addestramento", nullable = false)
    private Boolean addestramento;

}