import {getDatiCaratteristica, testoModificatore} from './Utils';

const REGEX_DADI = /^\d+d\d+([+-]\d+)?$/;
const REGEX_NUMERO = /^[+-]?\d+$/;

export function calcola(personaggio: any, formula: string): string {
    if (!personaggio || !formula) return '0';

    let evaluatedFormula = formula;

    const matches = formula.match(/@\w+/g);

    if (matches) {
        for (const match of matches) {
            const id = match.substring(1);
            const stat = getDatiCaratteristica(personaggio, id);
            const valore = parseInt(stat?.modificatore ?? '0', 10);

            const regex = new RegExp(match.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g');
            evaluatedFormula = evaluatedFormula.replace(regex, valore.toString());
        }
    }

    // Pulisci spazi
    evaluatedFormula = evaluatedFormula.replace(/\s+/g, '');

    // Caso 1: formula tipo "1d6+4"
    if (REGEX_DADI.test(evaluatedFormula)) {
        return evaluatedFormula;
    }

    // Caso 2: formula risultata in un numero puro (es. "4")
    if (REGEX_NUMERO.test(evaluatedFormula)) {
        return testoModificatore(Number(evaluatedFormula));
    }

    // Caso 3: formula complessa solo numerica → prova a valutarla
    try {
        // eslint-disable-next-line no-eval
        const result = eval(evaluatedFormula);
        if (typeof result === 'number' && !isNaN(result)) {
            return testoModificatore(result);
        }
    } catch (e) {
        console.warn('Errore nel calcolo formula:', formula, evaluatedFormula, e);
    }

    return '0';
}


export function getValoreLabel(personaggio, itemTarget: any, tipo: 'TPC' | 'TPD'): string | null {
    const label = itemTarget.labels?.find((l: any) => l.label === tipo);
    if (!label) return null;

    return calcola(personaggio, label.valore);
}







