package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class SpellBookDTO {
    Integer idClasse;
    String nomeClasse;
    String fonteTipo;  // TipoItem della fonte (CLASSE, OGGETTO, ...): distingue le sezioni in scheda
    String spellList;
    Integer casterLevel;   // classi: livello effettivo (+ prestigio); oggetti: SPELL_<n>_CASTER_LEVEL fisso
    String caratteristica; // stat id usata per la CD (SPELL_<n>_CARATTERISTICA / SP_CARATTERISTICA)
    Integer cd;            // 10 + casterLevel + modificatore caratteristica; null se non calcolabile
    Boolean mostraSimboliAzioni; // Mondo.mostraSimboliAzioni della fonte: icona invece di testo per il costo in azioni
    // Mondo.mostraCasterLevel della fonte: se false, il frontend non mostra "CL: X" in scheda (il
    // caster level resta comunque calcolato/usato per CD, slot e mana).
    Boolean mostraCasterLevel;
    // Indice "n" della sezione (SPELL_<n>...) sulla fonte (idClasse): serve al frontend per
    // aggiornare il contatore slot usati di un livello (vedi SpellBookLivelloDTO.slotConContatore).
    // Null sulle sezioni legacy (SPELL singola, senza sezioni indicizzate).
    Integer sezioneIndice;
    // Sezione con "classe di riferimento" (Constants.ITEM_LABEL_SPELL_CLASSE_RIF_SUFFIX, solo
    // oggetti): niente pool di slot separato, "slot" qui è già il numero di incantesimi
    // conosciuti/disponibili a quel livello — il frontend non deve mostrare "Slot: X" ma solo
    // "preparati/disponibili".
    Boolean soloConosciuti;
    // Sistema incantesimi del mondo della fonte (Constants.SISTEMA_INCANTESIMI_SLOT/MANA — vedi
    // Mondo.sistemaIncantesimi): se MANA, il frontend mostra un pool condiviso invece degli slot
    // per livello. formulaManaTotale è la formula del pool (Mondo.formulaManaIncantesimi),
    // valutata lato frontend con le stesse variabili delle formule bonus slot; manaUsati è il
    // consumo già tracciato per questa sezione (personaggio-scoped, un solo contatore).
    String sistemaIncantesimi;
    String formulaManaTotale;
    Integer manaUsati;
    List<SpellBookLivelloDTO> livelli;
    List<SpellBookIncantesimoDTO> spurii;  // incantesimi non da lista/catalogo, con utilizzi propri o di gruppo

    public SpellBookDTO() {
        livelli = new ArrayList<>();
        spurii = new ArrayList<>();
    }
}
