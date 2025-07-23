package it.fin8.grdsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

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
    @JoinColumn(name = "mondo_id")
    private Mondo mondo;

    @OneToMany(mappedBy = "personaggio", fetch = FetchType.LAZY)
    @Where(clause = "tipo = 'RAZZA'")
    @JsonIgnoreProperties("personaggio")
    private List<Item> razze;

    @OneToMany(mappedBy = "personaggio", fetch = FetchType.LAZY)
    @Where(clause = "tipo = 'ABILITA'")
    @JsonIgnoreProperties("personaggio")
    private List<Item> abilita;

    @OneToMany(mappedBy = "personaggio", fetch = FetchType.LAZY)
    @Where(clause = "tipo = 'TALENTO'")
    @JsonIgnoreProperties("personaggio")
    private List<Item> talenti;

    @OneToMany(mappedBy = "personaggio", fetch = FetchType.LAZY)
    @Where(clause = "tipo = 'OGGETTO'")
    @JsonIgnoreProperties("personaggio")
    private List<Item> oggetti;

    @OneToMany(mappedBy = "personaggio", fetch = FetchType.LAZY)
    @Where(clause = "tipo = 'CONSUMABILE'")
    @JsonIgnoreProperties("personaggio")
    private List<Item> consumabili;

    @OneToMany(mappedBy = "personaggio", fetch = FetchType.LAZY)
    @Where(clause = "tipo = 'ARMA'")
    @JsonIgnoreProperties("personaggio")
    private List<Item> armi;

    @OneToMany(mappedBy = "personaggio", fetch = FetchType.LAZY)
    @Where(clause = "tipo = 'MUNIZIONE'")
    @JsonIgnoreProperties("personaggio")
    private List<Item> munizioni;

    @OneToMany(mappedBy = "personaggio", fetch = FetchType.LAZY)
    @Where(clause = "tipo = 'EQUIPAGGIAMENTO'")
    @JsonIgnoreProperties("personaggio")
    private List<Item> equipaggiamento;

    @OneToMany(mappedBy = "personaggio", fetch = FetchType.LAZY)
    @Where(clause = "tipo = 'ALTRO'")
    @JsonIgnoreProperties("personaggio")
    private List<Item> altri;


    @OneToMany(mappedBy = "personaggio", fetch = FetchType.LAZY)
    @JsonIgnoreProperties("personaggio")
    private List<StatValue> stats;

    @OneToMany(mappedBy = "personaggio", fetch = FetchType.LAZY)
    @JsonIgnoreProperties("personaggio")
    private List<Livello> livello;


}