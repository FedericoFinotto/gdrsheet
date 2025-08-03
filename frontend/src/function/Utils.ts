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