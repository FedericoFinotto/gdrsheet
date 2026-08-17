package it.fin8.gdrsheet.def;

/**
 * Una "card" strutturale mostrabile in un editor item (fold/sezione legata a meccaniche vere:
 * attacchi, modificatori, incantesimi, abilità di classe...), abilitabile per (mondo, tipo item)
 * tramite {@link it.fin8.gdrsheet.entity.MondoTipoItemCardAbilitata}. I valori sono raggruppati per
 * "famiglia" di editor: quelli senza prefisso appartengono a BaseItemEditor.vue (27 tipi + Effetto/
 * Veicolo/Caso via BaseItemEditor diretto); {@code CLASSE_*} a ClasseEditor.vue (CLASSE/RAZZA);
 * {@code SPELL_*} a SpellEditor.vue (INCANTESIMO); {@code LIVELLO_*} a LivelloEditor.vue (LIVELLO).
 */
public enum CardEditorItem {
    QUANTITA,
    UTILIZZI_MAX,
    NOME_EN,
    MANUALE,
    DESCRITTORI_OGGETTO,
    INFO_OGGETTO,
    INFO_VEICOLO,
    DESCRITTORI_ABILITA,
    MONDO_SISTEMA,
    ATTACCHI,
    FORME,
    ITEM_COLLEGATI,
    EFFETTI,
    NOTE,
    IN_CARICO,
    INCANTESIMI,
    AGGIUNTA_CLASSE,
    LABELS,
    TAG,
    IMMAGINI,
    RANDOMIZZATORI_INNESCATI,
    MODIFICATORI,
    // Struttura ad albero (solo tipo NODO): collegamento al "contenuto" del nodo (un item di
    // qualunque tipo), campo Albero (label ALBERO_NODO) e i nodi successivi/predecessori — vedi
    // ItemService.applyNodoTipo/applyNodoA/applyNodoDa.
    NODO_STRUTTURA,
    // N sezioni, ciascuna con una lista di item candidati (label SCELTA_<n>_TITOLO/_CANDIDATI,
    // globali): quando un personaggio possiede l'item, indica per ciascuna sezione quale
    // candidato scelto (label SCELTA_<n>_FATTA, scoped per personaggio) — vedi ItemService.
    // setScelta/stampaLabelScopedPerPersonaggio e Mobile_DettaglioItem.vue. Aggiungibile a
    // qualunque tipo item, ma NON seedata come abilitata per i mondi esistenti: a differenza di
    // tutte le altre card, questa nasce disabilitata ovunque (va attivata a mano per mondo/tipo).
    SCELTE,
    // Campo "Reset" (label RESET, valore BREVE/LUNGO): indica quando si ripristinano gli
    // utilizzi consumati dell'item (Riposo Breve o Riposo Lungo). Puramente informativo (nessun
    // ripristino automatico), pensato per ABILITA ma aggiungibile a qualunque tipo. Nuova
    // funzionalità: NON seedata come abilitata per i mondi esistenti (opt-in, come SCELTE).
    RESET,

    // --- ClasseEditor.vue (CLASSE, RAZZA) --- Nome/Mondo-Sistema/Descrizione restano sempre
    // presenti, non toggle-abili; Nome EN/Manuale usano invece NOME_EN/MANUALE sopra (card
    // condivise con BaseItemEditor.vue, renderizzate anche qui).
    CLASSE_INFO_RAZZA,
    CLASSE_ABILITA,
    CLASSE_INCANTESIMI,
    CLASSE_TABELLA_LIVELLI,
    CLASSE_PRIVILEGI,
    // Sotto-campo "Età" DENTRO la card CLASSE_INFO_RAZZA: un mondo può voler mostrare Taglia/
    // Velocità/ecc. ma non l'Età. Non sostituisce CLASSE_INFO_RAZZA (che resta il toggle
    // dell'intera card).
    CLASSE_ETA,

    // --- SpellEditor.vue (INCANTESIMO) --- Nome/Tempo/TS/Range/Durata/Nome EN/Manuale/
    // Mondo-Sistema/Descrizione restano sempre presenti, non toggle-abili.
    SPELL_SCUOLE,
    SPELL_SOTTOSCUOLE,
    SPELL_DESCRITTORI,
    SPELL_CLASSI_DOMINI,
    SPELL_COMPONENTI,

    // --- LivelloEditor.vue (LIVELLO) --- numero livello + caratteristiche base restano sempre
    // presenti, non toggle-abili.
    LIVELLO_CLASSE_MALEDIZIONE,
    LIVELLO_DV_PF_GRADI,
    LIVELLO_CONTENUTI,
    LIVELLO_ITEM_EXTRA,
    LIVELLO_MODIFICATORI,
    LIVELLO_ABILITA_RANGHI,
    // Sotto-campo "Maledizione" DENTRO la card LIVELLO_CLASSE_MALEDIZIONE: un mondo può voler
    // mostrare la Classe/Razza ma non il campo Maledizione (es. non lo usa). Non sostituisce
    // LIVELLO_CLASSE_MALEDIZIONE (che resta il toggle dell'intera card, Classe compresa).
    LIVELLO_MALEDIZIONE,
    // Sotto-campo "Gradi (punti abilità)" DENTRO la riga LIVELLO_DV_PF_GRADI: stesso principio,
    // il valore continua a calcolarsi/congelarsi normalmente anche se il campo è nascosto.
    LIVELLO_GRADI,
    // Card che legge le Scelte definite sull'item Classe/Razza selezionato per questo livello
    // (label SCELTA_<n>_TITOLO/_CANDIDATI su quell'item) e permette al personaggio di
    // selezionarle subito al momento di prendere il livello, invece di doverlo fare separatamente
    // dal dettaglio dell'item — stessa scrittura di ItemService.setScelta (SCELTA_<n>_FATTA,
    // scoped sull'item Classe/Razza + questo personaggio). Aggiuntiva rispetto a SCELTE: NON
    // seedata come abilitata per i mondi esistenti (nuova funzionalità, opt-in).
    LIVELLO_SCELTE_CLASSE,

    // --- QuestEditor.vue/InfoEditor.vue (QUEST, INFO) --- oggi iniettate nello slot "specifico"
    // di BaseItemEditor.vue in modalità minimal; con l'editor unico diventano card come le altre.
    AMBITO,
    COMPLETATA,
    ARCHIVIATA,
}
