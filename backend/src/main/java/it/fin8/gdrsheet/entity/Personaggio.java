package it.fin8.gdrsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@Table(name = "personaggio")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Personaggio implements Serializable {
    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Size(max = 100)
    @NotNull
    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id")
    private Party party;

    @OneToMany(mappedBy = "personaggio", fetch = FetchType.LAZY)
    @JsonIgnoreProperties("personaggio")
    private List<Item> items;

    @OneToMany(mappedBy = "personaggio", fetch = FetchType.LAZY)
    @JsonIgnoreProperties("personaggio")
    private List<StatValue> stats;

    @OneToMany(mappedBy = "personaggio", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("personaggio")
    private List<PersonaggioLabel> labels;

}