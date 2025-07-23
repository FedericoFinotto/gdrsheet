package it.fin8.grdsheet.entity;

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
    private Integer valore;

}