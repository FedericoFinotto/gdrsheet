package it.fin8.gdrsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "personaggio_id", nullable = false)
    private Personaggio personaggio;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stat_id", nullable = false)
    private Stat stat;

    @NotNull
    @Column(name = "valore", nullable = false)
    private String valore;

    @NotNull
    @Column(name = "formula", nullable = false)
    private String formula;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mod", nullable = false)
    private Stat mod;

    @NotNull
    @Column(name = "classe", nullable = false)
    private Boolean classe;

    @NotNull
    @Column(name = "addestramento", nullable = false)
    private Boolean addestramento;

}