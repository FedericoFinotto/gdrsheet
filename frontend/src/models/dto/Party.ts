// Dettaglio party e soldi

export interface Soldi {
    mr: number;
    ma: number;
    mo: number;
    mp: number;
}

export interface PersonaggioSoldi {
    id: number;
    nome: string;
    soldi: Soldi;
    tipoPersonaggio?: 'NAVE' | 'STELLA' | string | null;
    proprietario: boolean;
    peso: number; // kg trasportati
}

export interface PartyDetail {
    id: number;
    nome: string;
    ruolo: 'MASTER' | 'GIOCATORE';
    personaggi: PersonaggioSoldi[];
    somma: Soldi;
    pesoTotale: number; // kg
}

export function formatKg(n: number): string {
    return `${(n ?? 0).toLocaleString('it-IT', {maximumFractionDigits: 2})} kg`;
}

// Controvalore totale in monete d'oro: 100 MR = 10 MA = 1 MO; 1 MP = 10 MO
export function totaleInMo(s: Soldi): number {
    return s.mr / 100 + s.ma / 10 + s.mo + s.mp * 10;
}
