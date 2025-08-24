package it.fin8.gdrsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.fin8.gdrsheet.StatDefault;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "mondo")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Mondo implements Serializable {

    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Size(max = 100)
    @NotNull
    @Column(name = "descrizione", nullable = false, length = 100)
    private String descrizione;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sistema_id")
    private Sistema sistema;

    @OneToMany(mappedBy = "mondo", fetch = FetchType.LAZY)
    @JsonIgnoreProperties("mondo")
    private List<StatDefault> defaultStats;

    @OneToMany(mappedBy = "mondo", fetch = FetchType.LAZY)
    @JsonIgnoreProperties("mondo")
    private List<Item> items;

}