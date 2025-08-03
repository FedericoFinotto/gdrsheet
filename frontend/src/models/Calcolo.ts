export interface CalcoloRequest {
    formula: string;
    datiPersonaggio: DatiPersonaggioDTO;
}

export interface CalcoloResponse {
    risultato: string;
    formula: string;
}