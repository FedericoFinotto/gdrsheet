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

    // --- ClasseEditor.vue (CLASSE, RAZZA) --- Nome/Nome EN/Manuale/Mondo-Sistema/Descrizione
    // restano sempre presenti, non toggle-abili (come Nome/Descrizione sopra).
    CLASSE_INFO_RAZZA,
    CLASSE_ABILITA,
    CLASSE_INCANTESIMI,
    CLASSE_TABELLA_LIVELLI,
    CLASSE_PRIVILEGI,

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

    // --- QuestEditor.vue/InfoEditor.vue (QUEST, INFO) --- oggi iniettate nello slot "specifico"
    // di BaseItemEditor.vue in modalità minimal; con l'editor unico diventano card come le altre.
    AMBITO,
    COMPLETATA,
    ARCHIVIATA,
}
