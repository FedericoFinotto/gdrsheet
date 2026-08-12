export interface CalcoloResponse {
    risultato: string;
    formula: string;
    // Formula con le variabili già sostituite dal backend (CalcoloService.sostituisciVariabili),
    // dadi/operatori lasciati come testo — usarla per la visualizzazione invece di ririsolvere le
    // variabili lato frontend.
    formulaRisolta: string;
    // Formula "leggibile" per il sotto-testo: solo le variabili opache (contatori item, taglia,
    // livelli per classe) sostituite col valore, le caratteristiche (FOR, SAG, ...) lasciate
    // simboliche — vedi CalcoloService.formulaLeggibile.
    formulaLeggibile: string;
}