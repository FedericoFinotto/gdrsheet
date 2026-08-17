// Catalogo delle "card" strutturali (CardEditorItem lato backend) e delle etichette leggibili,
// usato dalla UI admin di configurazione per-mondo (MondiAdmin.vue) per proporre, per un dato tipo
// item, quali card si possono abilitare/disabilitare. Rispecchia l'enum backend
// it.fin8.gdrsheet.def.CardEditorItem — se cambia là, va aggiornato anche qui.
import {TIPO_ITEM} from '../models/entity/ItemDB'

export const CARD_LABELS: Record<string, string> = {
    // BaseItemEditor.vue (famiglia "base", 27 tipi)
    QUANTITA: 'Quantità',
    UTILIZZI_MAX: 'Utilizzi massimi',
    NOME_EN: 'Nome (EN)',
    MANUALE: 'Manuale',
    DESCRITTORI_OGGETTO: 'Descrittori oggetto',
    INFO_OGGETTO: 'Info oggetto',
    INFO_VEICOLO: 'Info veicolo',
    DESCRITTORI_ABILITA: 'Descrittori abilità',
    MONDO_SISTEMA: 'Mondo/Sistema',
    ATTACCHI: 'Attacchi',
    FORME: 'Forme',
    ITEM_COLLEGATI: 'Item collegati',
    EFFETTI: 'Effetti',
    NOTE: 'Note',
    IN_CARICO: 'In carico',
    INCANTESIMI: 'Incantesimi (item)',
    AGGIUNTA_CLASSE: 'Aggiunta a classe',
    LABELS: 'Label',
    TAG: 'Tag',
    IMMAGINI: 'Immagini',
    RANDOMIZZATORI_INNESCATI: 'Randomizzatori innescati',
    MODIFICATORI: 'Modificatori',
    NODO_STRUTTURA: 'Struttura Nodo',
    SCELTE: 'Scelte',
    RESET: 'Reset (Riposo Breve/Lungo)',
    // ClasseEditor.vue (CLASSE, RAZZA)
    CLASSE_INFO_RAZZA: 'Info Razza',
    CLASSE_ABILITA: 'Abilità di classe',
    CLASSE_INCANTESIMI: 'Incantesimi (classe)',
    CLASSE_TABELLA_LIVELLI: 'Tabella livelli',
    CLASSE_PRIVILEGI: 'Privilegi di classe',
    CLASSE_ETA: 'Campo Età',
    // SpellEditor.vue (INCANTESIMO)
    SPELL_SCUOLE: 'Scuole',
    SPELL_SOTTOSCUOLE: 'Sottoscuole',
    SPELL_DESCRITTORI: 'Descrittori (incantesimo)',
    SPELL_CLASSI_DOMINI: 'Classi / Domìni',
    SPELL_COMPONENTI: 'Componenti',
    // LivelloEditor.vue (LIVELLO)
    LIVELLO_CLASSE_MALEDIZIONE: 'Classe / Maledizione',
    LIVELLO_DV_PF_GRADI: 'Dadi vita / Punti ferita / Gradi',
    LIVELLO_CONTENUTI: 'Contenuti del livello',
    LIVELLO_ITEM_EXTRA: 'Item aggiuntivi',
    LIVELLO_MODIFICATORI: 'Modificatori (livello)',
    LIVELLO_ABILITA_RANGHI: 'Abilità / Ranghi',
    LIVELLO_MALEDIZIONE: 'Campo Maledizione',
    LIVELLO_GRADI: 'Campo Gradi (punti abilità)',
    LIVELLO_SCELTE_CLASSE: 'Scelte della Classe/Razza',
    // Quest/Info (parte della famiglia base, si aggiungono solo per questi due tipi)
    AMBITO: 'Ambito',
    COMPLETATA: 'Completata',
    ARCHIVIATA: 'Archiviata',
}

const BASE_CARDS = [
    'QUANTITA', 'UTILIZZI_MAX', 'NOME_EN', 'MANUALE', 'DESCRITTORI_OGGETTO', 'INFO_OGGETTO',
    'INFO_VEICOLO', 'DESCRITTORI_ABILITA', 'MONDO_SISTEMA', 'ATTACCHI', 'FORME', 'ITEM_COLLEGATI',
    'EFFETTI', 'NOTE', 'IN_CARICO', 'INCANTESIMI', 'AGGIUNTA_CLASSE', 'LABELS', 'TAG', 'IMMAGINI',
    'RANDOMIZZATORI_INNESCATI', 'MODIFICATORI', 'NODO_STRUTTURA', 'SCELTE', 'RESET',
]
const AMBITO_CARDS = ['AMBITO', 'COMPLETATA', 'ARCHIVIATA']
// NOME_EN/MANUALE/ITEM_COLLEGATI/SCELTE sono card "base" ma disponibili anche qui: non c'è motivo
// per cui CLASSE/RAZZA non possano avere un nome originale disattivabile, un manuale, item
// collegati o sezioni di scelta come qualunque altro tipo — CLASSE/RAZZA usano ClasseEditor.vue
// (famiglia a parte) invece di BaseItemEditor.vue, ma queste 4 sono renderizzate lì comunque.
const CLASSE_CARDS = [
    'NOME_EN', 'MANUALE', 'CLASSE_INFO_RAZZA', 'CLASSE_ABILITA', 'CLASSE_INCANTESIMI',
    'CLASSE_TABELLA_LIVELLI', 'CLASSE_PRIVILEGI', 'ITEM_COLLEGATI', 'SCELTE', 'CLASSE_ETA',
]
const SPELL_CARDS = ['SPELL_SCUOLE', 'SPELL_SOTTOSCUOLE', 'SPELL_DESCRITTORI', 'SPELL_CLASSI_DOMINI', 'SPELL_COMPONENTI']
const LIVELLO_CARDS = [
    'LIVELLO_CLASSE_MALEDIZIONE', 'LIVELLO_DV_PF_GRADI', 'LIVELLO_CONTENUTI', 'LIVELLO_ITEM_EXTRA',
    'LIVELLO_MODIFICATORI', 'LIVELLO_ABILITA_RANGHI', 'LIVELLO_MALEDIZIONE', 'LIVELLO_GRADI',
    'LIVELLO_SCELTE_CLASSE',
]

// Card proponibili per un dato tipo item, in base alla famiglia di editor che lo gestisce
// (vedi editorRegistry.ts). Nota: un admin può abilitare qualunque card della famiglia "base"
// per qualunque tipo base — è il punto del sistema (non ci sono più regole hardcoded per tipo).
export function cardsForTipo(tipo: string): string[] {
    if (tipo === TIPO_ITEM.CLASSE || tipo === TIPO_ITEM.RAZZA) return CLASSE_CARDS
    if (tipo === TIPO_ITEM.INCANTESIMO) return SPELL_CARDS
    if (tipo === TIPO_ITEM.LIVELLO) return LIVELLO_CARDS
    if (tipo === TIPO_ITEM.QUEST || tipo === TIPO_ITEM.INFO) return [...BASE_CARDS, ...AMBITO_CARDS]
    return BASE_CARDS
}
