import {calcolaFormula} from "../service/PersonaggioService";

import {DatiPersonaggio} from "../models/dto/DatiPersonaggio";

const REGEX_DADI = /^\d+d\d+([+-]\d+)?$/;
const REGEX_NUMERO = /^[+-]?\d+$/;

export function getValoreLabel(personaggio, itemTarget: any, tipo: 'TPC' | 'TPD' | 'TS') {
    const label = itemTarget.labels?.find((l: any) => l.label === tipo);
    if (!label) return null;
    return calcolaFormula(label.valore, personaggio.modificatori);
}

export function getLabel(personaggio, itemTarget: any, tipo: 'TPC' | 'TPD' | 'TS') {
    const label = itemTarget.labels?.find((l: any) => l.label === tipo);
    if (!label) return null;
    return label.valore;
}

export function getItemLabel(itemTarget: any, label: string) {
    const lab = itemTarget.labels?.find((l: any) => l.label === label);
    if (!lab) return null;
    return lab.valore;
}

export function getValoreFormula(personaggio: DatiPersonaggio, formula: string) {
    if (!formula) return null;
    return calcolaFormula(formula, personaggio);
}

export function thereIsValoreLabel(personaggio, itemTarget: any, tipo: 'TPC' | 'TPD'): boolean {
    const prova = itemTarget.labels?.find((l: any) => l.label === tipo);
    return prova !== null;
}







