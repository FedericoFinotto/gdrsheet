package it.fin8.gdrsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "collegamento")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Collegamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_item_source", nullable = false)
    @JsonIgnoreProperties("personaggio")
    private Item itemSource;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_item_target", nullable = false)
    @JsonIgnoreProperties("personaggio")
    private Item itemTarget;

    @OneToMany(mappedBy = "collegamento", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("collegamento")
    private List<CollegamentoLabel> labels = new ArrayList<>();

    /* ===================== Helper labels ===================== */

    /**
     * Ritorna il valore della label 'key', oppure null se non presente.
     */
    public String getLabel(String key) {
        if (key == null || labels == null) return null;
        for (CollegamentoLabel l : labels) {
            if (Objects.equals(key, l.getLabel())) {
                return l.getValore();
            }
        }
        return null;
    }

    /**
     * Imposta/aggiorna la label 'key' con 'value'.
     * Se 'value' è null, la label viene rimossa.
     * Restituisce this per chaining.
     */
    public void setLabel(String key, String value) {
        if (key == null) return;

        if (value == null) {
            removeLabel(key);
            return;
        }

        if (labels == null) labels = new ArrayList<>();

        for (CollegamentoLabel l : labels) {
            if (Objects.equals(key, l.getLabel())) {
                l.setValore(value);
                return;
            }
        }

        // non esiste: crea nuova label
        CollegamentoLabel nl = new CollegamentoLabel();
        nl.setCollegamento(this);  // back-reference per orphanRemoval
        nl.setLabel(key);
        nl.setValore(value);
        labels.add(nl);
    }

    /**
     * Rimuove la label 'key' se presente.
     */
    public void removeLabel(String key) {
        if (key == null || labels == null) return;
        boolean removed = false;
        for (Iterator<CollegamentoLabel> it = labels.iterator(); it.hasNext(); ) {
            CollegamentoLabel l = it.next();
            if (Objects.equals(key, l.getLabel())) {
                it.remove();                 // orphanRemoval -> delete
                l.setCollegamento(null);     // pulizia back-ref
                removed = true;
            }
        }
    }

    /* ===================== Utility esistenti ===================== */

    /**
     * Confronto "logico" basato su source/target. NON override di Object.equals.
     */
    public Boolean stessoCollegamento(Collegamento collegamento) {
        if (collegamento == null) return false;
        if (this.itemSource == null || this.itemTarget == null
                || collegamento.getItemSource() == null || collegamento.getItemTarget() == null) {
            return false;
        }
        return this.itemSource.getId().equals(collegamento.getItemSource().getId())
                && this.itemTarget.getId().equals(collegamento.getItemTarget().getId());
    }

    public Boolean contenutoIn(List<Collegamento> collegamentos) {
        if (collegamentos == null) return false;
        for (Collegamento collegamento : collegamentos) {
            if (this.equals(collegamento)) {
                return true;
            }
        }
        return false;
    }
}
