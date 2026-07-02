import {IconKey} from "../components/Icona/ListaIcone";
import {ItemDB, TIPO_ITEM} from "../models/entity/ItemDB";
import {Modificatore} from "../models/entity/Modificatore";
import {Avanzamento} from "../models/entity/Avanzamento";
import {LABELS} from "../models/entity/ItemLabel";
import {DatiPersonaggio} from "../models/dto/DatiPersonaggio";

const REGEX_DICE = /^\d+d\d+(?:[+-]\d+)?$/i;

export function testoModificatoreConTipo(mod: number | string, tipo?: string): string {
    if (tipo === 'MOLTIPLICA') return `×${mod}`;
    if (tipo === 'DIVIDI') return `/${mod}`;
    return testoModificatore(mod);
}

export function testoModificatore(mod: number | string): string {
    const str = typeof mod === 'number' ? String(mod) : mod;

    // se è un tiro di dado (es. "1d6", "2d8+3", "10d10-2") restituisci invariato
    if (REGEX_DICE.test(str)) {
        return str;
    }

    // altrimenti prova a interpretarlo come numero
    const num = Number(str);
    if (!isNaN(num)) {
        return num >= 0 ? `+${num}` : `${num}`;
    }

    // fallback: ritorna la stringa così com'è
    return str;
}

// Somma `bonus` a ogni numero intero presente nel testo. Una volta sommato il
// tiro il valore è "secco", quindi NON si antepone il segno + (resta solo il
// segno - per i negativi): es. "+6 / +1" con bonus 15 -> "21 / 16".
// Le notazioni di dado (es. "1d6") restano invariate.
export function applicaBonusDado(testo: string, bonus: number): string {
    if (!bonus || !testo) return testo;
    if (REGEX_DICE.test(testo.trim())) return testo;
    return testo.replace(/[+-]?\d+/g, (m) => String(Number(m) + bonus));
}

export function removePlus(valore: string) {
    return valore.replace('+', '');
}

/**
 * Risolve le variabili simboliche nella formula danno usando i valori numerici del personaggio.
 * Es: "16d8+14d6+MSCd10+FOR" → "16d8+14d6+171d10+169"
 */
export function risolviFormulaDanno(formula: string, stats: DatiPersonaggio): string {
    const vars: Record<string, number> = {}
    for (const c of stats.caratteristiche ?? []) vars[c.id] = c.modificatore
    for (const b of stats.bonusAttacco ?? []) vars[b.id] = b.attacchiMultipli?.[0] ?? b.modificatore
    for (const t of stats.tiriSalvezza ?? []) vars[t.id] = t.modificatore
    for (const a of stats.attributi ?? []) vars[a.id] = a.modificatore
    for (const ca of stats.classeArmatura ?? []) vars[ca.id] = ca.modificatore
    // variabili anagrafiche/peso disponibili nelle formule
    const info = stats.info ?? {}
    const numInfo = (k: string) => {
        const n = parseFloat(String(info[k] ?? '').replace(',', '.'))
        return isNaN(n) ? undefined : n
    }
    if (numInfo('PESO') !== undefined) vars['PESO'] = numInfo('PESO')!
    if (numInfo('ETA') !== undefined) vars['ETA'] = numInfo('ETA')!
    if (numInfo('ALTEZZA') !== undefined) vars['ALTEZZA'] = numInfo('ALTEZZA')!

    return formula
        .replace(/@/g, '')
        .replace(/\*/g, '')
        .replace(/0\.5/g, '½')
        .replace(/[A-Z]{2,}/g, match => {
            const val = vars[match]
            return val !== undefined ? String(val) : match
        })
}

export function testoFormula(formula: string): string {
    return formula
        .replace(/@/g, '')        // rimuove tutte le @
        .replace(/\*/g, '')      // sostituisce tutti i * con 'x'
        .replace(/0\.5/g, '½')   // sostituisce tutti i 0.5 con ½
        .replace('MSC', 'Mischia')
        .replace('GTT', 'Distanza')
        .replace('INF', '∞')
        ;
}

// D&D 3.5 — mappa taglie (IT) centrata su Medium=0
const SIZE_IT = new Map<number, string>([
    [-4, 'Piccolissima'], // Fine
    [-3, 'Minuta'],       // Diminutive
    [-2, 'Minuscola'],    // Tiny
    [-1, 'Piccola'],      // Small
    [0, 'Media'],        // Medium
    [1, 'Grande'],       // Large
    [2, 'Enorme'],       // Huge
    [3, 'Mastodontica'], // Gargantuan
    [4, 'Colossale'],    // Colossal
]);

/**
 * Converte un codice di taglia (stringa o numero) nella label italiana.
 * Esempi: "0" -> "Media", "1" -> "Grande", "-1" -> "Piccola"
 */
export function testoTaglia(code: string | number): string {
    const n = typeof code === 'number'
        ? code
        : parseInt(String(code).trim(), 10);

    if (Number.isNaN(n)) return 'Sconosciuta';
    return SIZE_IT.get(n) ?? 'Sconosciuta';
}

type LabeledValue = { label: string; value: string };

export function mostraLabel(label: string, val: string): LabeledValue | null {
    const v = String(val ?? '').trim();

    switch (label) {
        case LABELS.SPELL_TEMPO:
            return {label: 'Azione', value: v};

        case LABELS.SPELL_RANGE:
            return {label: 'Range', value: v};

        case LABELS.SPELL_DURATA:
            return {label: 'Durata', value: v};

        case LABELS.SPELL_TIRO_SALVEZZA:
            if (v.toLowerCase() === 'none' || v === '') return null; // nascondi se "None"
            return {label: 'Tiro Salvezza', value: v};

        case LABELS.SPELL_SCUOLA:
            return {label: 'Scuola', value: v};

        case LABELS.TAGLIA:
            return {label: 'Taglia', value: testoTaglia(v)};

        case LABELS.SPELL_LISTA:
            return {label: 'Lista Incantesimi', value: v};

        default:
            return null; // non mostrare
    }
}

export function iconForComponent(raw: string): IconKey {
    const t = String(raw).trim().toUpperCase();
    // console.log('icona', t);
    switch (t) {
        case 'V':
            return 'COMP_V';
        case 'S':
            return 'COMP_S';
        case 'M':
            return 'COMP_M';
        case 'F':
            return 'COMP_F';
        case 'DF':
            return 'COMP_DF';
        case 'XP':
            return 'COMP_XP';
        case 'X':
            return 'COMP_X';
        case 'Corrupt':
            return 'COMP_CORRUPT';
        case 'Coldfire':
            return 'COMP_COLDFIRE';
        case 'Frostfell':
            return 'COMP_FROSTFELL';
        case 'B':
            return 'COMP_BREATH';        // “Breath” (soffio)
        default:
            return 'COMP_M';              // fallback a qualcosa di neutro
    }
}

function toLevel(a: Avanzamento): number {
    const n = Number.parseInt((a as any).livello ?? '', 10);
    return Number.isFinite(n) ? n : 0;
}

function hasDerivedItem(a: Avanzamento): boolean {
    const tgt: any = (a as any).itemTarget ?? (a as any).item ?? null;
    return !!tgt && tgt.tipo !== TIPO_ITEM.AVANZAMENTO;
}

function hasDerivedMod(a: Avanzamento): boolean {
    const tgt: any = (a as any).itemTarget ?? (a as any).item ?? null;
    return !!tgt && tgt.modificatori.length > 0;
}

export function buildMappaItemAvanzamenti(list: Avanzamento[] | null | undefined, livello: number[] = null): Record<number, Avanzamento[]> {
    if (!Array.isArray(list) || list.length === 0) return {};

    return list.filter(x => livello === null || livello.includes(x.livello)).reduce<Record<number, Avanzamento[]>>((acc, a) => {
        if (!hasDerivedItem(a)) return acc;

        const lvl = toLevel(a);
        if (!Number.isFinite(lvl)) return acc;

        (acc[lvl] ??= []).push(a);
        return acc;
    }, {});
}

function getModsFromAvanzamento(a: Avanzamento): Modificatore[] {
    // prendi i modificatori dell'item derivato; aggiungi altri fallback se servono
    const target: ItemDB | undefined = (a as any).itemTarget ?? (a as any).item
    const mods = target?.modificatori ?? []
    // se l'itemTarget è di tipo AVANZAMENTO o non ha mods, ritorna []
    if (!mods.length) return []
    return mods
}

export function buildMappaModificatoriAvanzamenti(
    list: Avanzamento[] | null | undefined,
    livello: number[] = null
): Record<number, Modificatore[]> {
    const out: Record<number, Modificatore[]> = {}
    if (!Array.isArray(list) || list.length === 0) return out

    for (const a of list.filter(x => livello === null || livello.includes(x.livello))) {
        const lvl = toLevel(a)
        if (!Number.isFinite(lvl)) continue

        const mods = getModsFromAvanzamento(a)
        if (!mods.length) continue

            ;
        (out[lvl] ??= []).push(...mods)
    }
    return out
}


