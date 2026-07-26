package it.fin8.gdrsheet.dto;

import it.fin8.gdrsheet.entity.Item;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Comparator;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InfoClasseDTO {
    Item classe;
    Set<Integer> livelli;
    Integer livelloNonMaledetto;
    Integer livelloMaxNonMaledetto;
    Integer livelloTotale;
    Integer livelloMax;
    // Caster level di questa classe (usato per lo spellbook), comprensivo dei livelli "aggiunti"
    // da altre classi di prestigio che avanzano la sua stessa lista incantesimi (liste "+<lista>").
    // Calcolati in PersonaggioService.computeCasterLevel, dopo applyAddClasseLevels.
    Integer casterLevelNonMaledetto; // conta solo i livelli non maledetti (come lo spellbook)
    Integer casterLevelTotale;       // conta anche i livelli maledetti

    // --- Supporto ad ADD_CLASSE_<n>: di default un ADD_CLASSE non aggiunge NULLA, ogni effetto è
    // opt-in tramite il proprio flag (_LIVELLO/_ITEMS/_BONUS/_SPELL), tra loro indipendenti — un
    // ADD_CLASSE con solo _SPELL=1 conta per gli incantesimi ma non compare da nessun'altra parte
    // (non nella lista livelli, non nelle variabili @LIVELLO_*, niente privilegi/BAB/TS). Per questo
    // livelloNonMaledetto/livelloTotale/livelloMaxNonMaledetto/livelli sopra riflettono SOLO i
    // livelli reali più quelli virtuali con _LIVELLO=1; il contributo di _SPELL=1 (che può avere o
    // non avere anche _LIVELLO=1) è tenuto qui separato per non essere né perso né contato due volte.

    // Snapshot dei livelli REALI (pre-ADD_CLASSE), impostato in applyAddClasseLevels prima di
    // qualunque mutazione: base per il calcolo incantesimi, indipendente da cosa _LIVELLO abbia
    // eventualmente aggiunto sopra.
    Integer livelloNonMaledettoRealeBase;
    Integer livelloTotaleRealeBase;
    Integer livelloMaxNonMaledettoRealeBase;

    // Somma di "extra" di ogni ADD_CLASSE_<n> con _SPELL=1 (a prescindere da _LIVELLO).
    Integer virtualiSpellNonMaledetto;
    Integer virtualiSpellTotale;
    Integer virtualiSpellMaxNonMaledetto;

    public Integer getLivelloNonMaledettoPerIncantesimi() {
        return sommaBaseESpell(livelloNonMaledettoRealeBase, virtualiSpellNonMaledetto);
    }

    public Integer getLivelloTotalePerIncantesimi() {
        return sommaBaseESpell(livelloTotaleRealeBase, virtualiSpellTotale);
    }

    public Integer getLivelloMaxNonMaledettoPerIncantesimi() {
        return sommaBaseESpell(livelloMaxNonMaledettoRealeBase, virtualiSpellMaxNonMaledetto);
    }

    private static Integer sommaBaseESpell(Integer base, Integer virtualiSpell) {
        int b = base != null ? base : 0;
        int v = virtualiSpell != null ? virtualiSpell : 0;
        return b + v;
    }

    public Integer getMax() {
        if (livelli == null || livelli.isEmpty()) return 0;
        return livelli
                .stream()
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    public Integer getNumber() {
        if (livelli == null || livelli.isEmpty()) return 0;
        return livelli.size();
    }
}
