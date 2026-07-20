export interface Segnalazione {
    id: number;
    ref: number;
    titolo: string;
    descrizione: string | null;
    stato: string | null;
    dataCreazione: string | null;
    dataModifica: string | null;
}

export interface Comento {
    autore: string | null;
    testo: string;
    data: string | null;
}
