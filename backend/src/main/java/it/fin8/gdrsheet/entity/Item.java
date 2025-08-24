package it.fin8.gdrsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.fin8.gdrsheet.def.TipoItem;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

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
    @JsonIgnoreProperties("items")
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

    public String getLabel(String key) {
        if (key == null || labels == null) return null;
        for (ItemLabel l : labels) {
            if (Objects.equals(key, l.getLabel())) {
                return l.getValore();
            }
        }
        return null;
    }
    
    public void setLabel(String key, String value) {
        if (key == null) return;

        if (value == null) {
            removeLabel(key);
            return;
        }

        if (labels == null) labels = new ArrayList<>();

        for (ItemLabel l : labels) {
            if (Objects.equals(key, l.getLabel())) {
                l.setValore(value);
                return;
            }
        }

        // non esiste: crea nuova label
        ItemLabel nl = new ItemLabel();
        nl.setItem(this);  // back-reference per orphanRemoval
        nl.setLabel(key);
        nl.setValore(value);
        labels.add(nl);
    }

    public void removeLabel(String key) {
        if (key == null || labels == null) return;
        boolean removed = false;
        for (Iterator<ItemLabel> it = labels.iterator(); it.hasNext(); ) {
            ItemLabel l = it.next();
            if (Objects.equals(key, l.getLabel())) {
                it.remove();                 // orphanRemoval -> delete
                l.setItem(null);     // pulizia back-ref
                removed = true;
            }
        }
    }

    public Collegamento getChildByType(TipoItem type) {
        return child.stream().filter(x -> x.getItemTarget().getTipo().equals(type)).findFirst().orElse(null);
    }

}