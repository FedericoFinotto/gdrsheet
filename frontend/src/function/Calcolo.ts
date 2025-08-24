import {calcolaFormula} from "../service/PersonaggioService";

import {DatiPersonaggio} from "../models/dto/DatiPersonaggio";

// const REGEX_DADI = /^\d+d\d+([+-]\d+)?$/;
// const REGEX_NUMERO = /^[+-]?\d+$/;

export function getValoreFormula(personaggio: DatiPersonaggio, formula: string) {
    if (!formula) return null;
    return calcolaFormula(formula, personaggio);
}







