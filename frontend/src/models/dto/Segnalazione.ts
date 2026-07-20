export interface Segnalazione {
    id: number;
    ref: number;
    titolo: string;
    descrizione: string | null;
    stato: string | null;
    dataCreazione: string | null;
    dataModifica: string | null;
    mia: boolean;
}

export interface Comento {
    autore: string | null;
    testo: string;
    data: string | null;
}

export interface Allegato {
    id: number;
    nome: string | null;
    url: string | null;
}
