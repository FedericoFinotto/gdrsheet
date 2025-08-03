package it.fin8.grdsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.fin8.grdsheet.def.TipoItem;
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
@Table(name = "items")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "personaggio", "parent"})
public class Item implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Size(max = 100)
    @NotNull
    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "tipo", nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoItem tipo;

    @Column(name = "descrizione", length = Integer.MAX_VALUE)
    private String descrizione;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personaggio_id")
    private Personaggio personaggio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sistema")
    private Sistema sistema;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_mondo")
    private Mondo mondo;

    @OneToMany(mappedBy = "itemSource", fetch = FetchType.LAZY)
    @JsonIgnoreProperties("itemSource")
    private List<Collegamento> child;

    @OneToMany(mappedBy = "itemTarget", fetch = FetchType.LAZY)
    @JsonIgnoreProperties("itemTarget")
    private List<Collegamento> parent;

    @OneToMany(mappedBy = "item", fetch = FetchType.LAZY)
    @JsonIgnoreProperties("idItem")
    private List<Modificatore> modificatori;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("item")
    private List<ItemLabel> labels;

    @OneToMany(mappedBy = "itemSource", fetch = FetchType.LAZY)
    @JsonIgnoreProperties("itemSource")
    private List<Avanzamento> avanzamento;

}