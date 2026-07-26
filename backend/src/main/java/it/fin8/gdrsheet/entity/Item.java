package it.fin8.gdrsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.fin8.gdrsheet.config.Constants;
import it.fin8.gdrsheet.def.TipoItem;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Where;

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
    @Where(clause = "id_personaggio IS NULL")
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

    public Modificatore getModificatore(String key) {
        if (key == null || modificatori == null) return null;
        for (Modificatore m : modificatori) {
            if (Objects.equals(key, m.getStat().getId())) {
                return m;
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

    public Boolean isDisabled() {
        String disabledLabel = getLabel(Constants.ITEM_LABEL_DISABILITATO);
        return disabledLabel != null && disabledLabel.equals("1");
    }

    /**
     * Navigazione strutturale verso il genitore (usata es. da $M_P_&lt;var&gt; per trovare il
     * frutto a cui applicare la variabile di una forma): NON filtra più per "disabilitato" — da
     * quando DISABLED è diventato sempre personaggio-scoped (vedi ItemService.isItemDisabled),
     * Collegamento.isDisabled() non è mai più scritto (sempre false) e Item.isDisabled() legge
     * solo la label globale, che qui non riflette lo stato per il personaggio corrente: il
     * risultato era che un frutto/forma "normalmente abilitato" per il personaggio veniva escluso
     * comunque, rompendo la risoluzione di $M_P_FORMA per qualunque forma diversa dalla prima.
     */
    public Item getFirstParent(TipoItem type) {
        if (parent.isEmpty()) return null;
        if (type == null)
            return parent.get(0).getItemSource();
        else
            return parent.stream().map(Collegamento::getItemSource).filter(itemSource -> itemSource.getTipo().equals(type)).findFirst().orElse(null);
    }

    /** Stesso motivo di {@link #getFirstParent}: nessun filtro su "disabilitato". */
    public Item getFirstChild(TipoItem type) {
        if (child.isEmpty()) return null;
        if (type == null)
            return child.get(0).getItemSource();
        else
            return child.stream().map(Collegamento::getItemSource).filter(itemSource -> itemSource.getTipo().equals(type)).findFirst().orElse(null);
    }

}