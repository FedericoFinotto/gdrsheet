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
    TIRO_DANNI: 'TPD',
    TIRO_COLPIRE: 'TPC',
    CLASSE_LIVELLO: 'LVL_CLASSE',
    SPELL_TEMPO: 'TEMPO_SP',
    SPELL_SCUOLA: 'SCUOLA_SP',
    LIVELLO: 'LVL',
    SPELL_SLOT: 'SP_SLOT',
    ABILITA_CLASSE: 'ABCLASSE',
    CLASSE: 'CLASSE',
    MALEDIZIONE: 'MLDZN'
} as const;
export type TipoLabels = typeof LABELS[keyof typeof LABELS];

export function thereIsValoreLabel(itemTarget: ItemDB, tipo: TipoLabels): boolean {
    const prova = itemTarget.labels?.find((l: any) => l.label === tipo);
    return prova !== null;
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