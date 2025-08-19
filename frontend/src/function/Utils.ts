import {IconKey} from "../components/Icona/ListaIcone";

const REGEX_DICE = /^\d+d\d+(?:[+-]\d+)?$/i;

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

export function testoFormula(formula: string): string {
    return formula
        .replace(/@/g, '')        // rimuove tutte le @
        .replace(/\*/g, '')      // sostituisce tutti i * con 'x'
        .replace(/0\.5/g, '½')   // sostituisce tutti i 0.5 con ½
        .replace('MSC', 'Mischia')
        .replace('GTT', 'Distanza')
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
        case 'TEMPO_SP':
            return {label: 'Azione', value: v};

        case 'RANGE_SP':
            return {label: 'Range', value: v};

        case 'DURATA_SP':
            return {label: 'Durata', value: v};

        case 'TS_SP':
            if (v.toLowerCase() === 'none' || v === '') return null; // nascondi se "None"
            return {label: 'Tiro Salvezza', value: v};

        case 'SCUOLA_SP':
            return {label: 'Scuola', value: v};

        case 'TAGLIA':
            return {label: 'Taglia', value: testoTaglia(v)};

        default:
            return null; // non mostrare
    }
}

export function iconForComponent(raw: string): IconKey {
    const t = String(raw).trim().toUpperCase();
    console.log('icona', t);
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


