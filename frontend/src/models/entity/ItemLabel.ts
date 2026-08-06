// ItemLabel
import {ItemDB} from "./ItemDB";

export interface ItemLabel {
    id: number;
    // item: ItemDB;
    label?: string;
    valore?: string;
}

export const LABELS = {
    LINGUE: 'LINGUE',
    SPELL_LISTA: 'SPELL',
    COMPETENZE: 'COMP',
    SPELL_DURATA: 'DURATA_SP',
    SPELL_MANUALE: 'MANUALE_SP',
    SPELL_RANGE: 'RANGE_SP',
    SPELL_COMPONENTE: 'COMP_SP',
    SPELL_TIRO_SALVEZZA: 'TS_SP',
    SPELL_RESISTENZA: 'RES_SP',
    TAGLIA: 'TAGLIA',
    // Taglia fisica dell'oggetto (es. arma taglia Grande): puramente descrittiva, diversa da TAGLIA
    // sopra che invece imposta la taglia del personaggio.
    TAGLIA_OGGETTO: 'TAGLIA_OGGETTO',
    // Prefisso mostrato come chip prima del nome nell'inventario, sugli item collegati (child)
    // di questo oggetto (es. una faretra con prefisso "Freccia" sulle sue frecce).
    PREFISSO_OGGETTI: 'PREFISSO_OGGETTI',
    // Info Veicolo: campi descrittivi, valorizzati solo per item di tipo VEICOLO.
    VEICOLO_VELOCITA: 'VEICOLO_VELOCITA',
    // Info Razza: campi puramente descrittivi, valorizzati solo per item di tipo RAZZA.
    RAZZA_TAGLIA: 'RAZZA_TAGLIA',
    RAZZA_VELOCITA: 'RAZZA_VELOCITA',
    RAZZA_CARATTERISTICHE: 'RAZZA_CARATTERISTICHE',
    RAZZA_LAP: 'RAZZA_LAP',
    RAZZA_SPAZIO: 'RAZZA_SPAZIO',
    RAZZA_PORTATA: 'RAZZA_PORTATA',
    // Razza (import bulk da dndtools.org, vedi scripts/dndtools-scraper/scrape_races.py)
    RAZZA_LINGUE_AUTOMATICHE: 'RAZZA_LINGUE_AUTOMATICHE',
    RAZZA_CLASSE_PREFERITA: 'RAZZA_CLASSE_PREFERITA',
    RAZZA_LINGUE_BONUS: 'RAZZA_LINGUE_BONUS',
    TIRO_DANNI: 'TPD',
    TIRO_COLPIRE: 'TPC',
    CLASSE_LIVELLO: 'LVL_CLASSE',
    SPELL_TEMPO: 'TEMPO_SP',
    SPELL_SCUOLA: 'SCUOLA_SP',
    LIVELLO: 'LVL',
    SPELL_SLOT: 'SP_SLOT',
    ABILITA_CLASSE: 'ABCLASSE',
    CLASSE: 'CLASSE',
    MALEDIZIONE: 'MLDZN',
    // Talento (import bulk da dndtools.org, vedi scripts/dndtools-scraper)
    EN_NAME: 'EN_NAME',
    MANUALE: 'MANUALE_SP',
    PAGE: 'PAGE',
    LINK: 'LINK',
    CATEGORY: 'CATEGORY',
    PREREQUISITE: 'PREREQUISITE',
    REQUIRED_FOR: 'REQUIRED_FOR',
    BENEFIT: 'BENEFIT',
    SPECIAL: 'SPECIAL',
    NORMAL: 'NORMAL',
    EXTRA: 'EXTRA',
    // Descrittori Oggetto
    MAGICO: 'MAGICO',
    PSIONICO: 'PSIONICO',
    DIVINO: 'DIVINO',
    LEGGENDARIO: 'LEGGENDARIO',
    UNICO: 'UNICO',
    COSTO: 'COSTO',
    MATERIALE: 'MATERIALE',
    // Descrittori Abilità
    DESCR_STRAORDINARIA: 'DESCR_STR',
    DESCR_MAGICA: 'DESCR_MAG',
    DESCR_SOPRANNATURALE: 'DESCR_SOP',
    DESCR_NATURALE: 'DESCR_NAT',
    DESCR_DIVINA: 'DESCR_DIV',
    // Privilegio di Classe: raggruppa versioni dello stesso privilegio che si "aggiornano" a
    // livello più alto (es. sbloccata da una classe di prestigio) — vedi PrivilegioEditor.vue.
    GRUPPO_PRIVILEGI: 'GRUPPO_PRIVILEGI',
    // Attacco: TPC/TPD sopra (TIRO_COLPIRE/TIRO_DANNI), qui il resto dei campi (AttaccoEditor.vue).
    ATTACCO_TDANNO: 'TDANNO',
    ATTACCO_TRANGE: 'TRANGE',
    ATTACCO_TTS: 'TTS',
    // Arma/Munizione: competenza richiesta per non subire penalità (ArmaEditor/MunizioneEditor).
    REQ_COMP: 'REQ_COMP',
    // Contenitore: sezione a parte in scheda invece di finire nel pool normale del personaggio
    // (ContenitoreEditor.vue).
    INVENTARIO_SEPARATO: 'INVENTARIO_SEPARATO',
} as const;
export type TipoLabels = typeof LABELS[keyof typeof LABELS];

export function thereIsValoreLabel(itemTarget: ItemDB, tipo: TipoLabels): boolean {
    const prova = itemTarget.labels?.find((l: any) => l.label === tipo);
    if (prova === undefined) return false;
    const v = String(prova.valore ?? '').trim().toLowerCase();
    return v !== '' && v !== '0' && v !== 'false';
}

export function getItemLabels(itm: ItemDB, label: TipoLabels): string[] {
    const lab = itm.labels?.filter((l: ItemLabel) => l.label === label);
    if (!lab || lab.length === 0) return null;
    return lab.map(x => x.valore);
}

export function getItemLabel(itm: ItemDB, label: TipoLabels): string {
    const lab = getItemLabels(itm, label);
    if (!lab || lab.length === 0) return null;
    return lab[0];
}