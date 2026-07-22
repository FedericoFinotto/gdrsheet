export interface DannoAttacco {
    formula: string;
    tipo?: string;
}

export interface Attacco {
    id?: number;
    nome?: string;
    tipoRisoluzione?: string; // "TPC" | "TS"
    attacco: string;          // formula TPC, solo se tipoRisoluzione = TPC
    nomeItem: string;
    tiroSalvezza: string;     // tipo di TS, solo se tipoRisoluzione = TS
    tiroSalvezzaCd?: string;  // formula CD, solo se tipoRisoluzione = TS
    range: string;
    danni: DannoAttacco[];
}