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
    pesoMonete: number; // kg, già incluso nel totale
}

export interface PartyItem {
    id: number;
    nome: string;
    tipo: string;
    peso: number;       // kg complessivi (unitario x quantità)
    quantita: number;
    personaggioId: number;
    personaggioNome: string;
    disabled: boolean;
}

export interface Page<T> {
    content: T[];
    page: number;        // 0-based
    size: number;
    totalElements: number;
    totalPages: number;
}

export interface Conto {
    itemId: number;
    cc: string;                  // G<idPersonaggio> | P<idParty>
    tipo: 'GIOCATORE' | 'PARTY';
    intestatarioId: number;
    intestatarioNome: string;
    soldi: Soldi;
}

export interface Banca {
    personaggioId: number;
    nome: string;
    conti: Conto[];
}

export interface GruppoPartyBanca {
    partyId: number | null;
    partyNome: string;
    totale: Soldi;
    conti: Conto[];
}

export interface BancaDetail {
    personaggioId: number;
    nome: string;
    totale: Soldi;
    gruppi: GruppoPartyBanca[];
}

export function formatKg(n: number): string {
    return `${(n ?? 0).toLocaleString('it-IT', {maximumFractionDigits: 2})} kg`;
}

// per i totali: sopra le 10.000 t solo tonnellate, sotto solo kg
const SOGLIA_TONNELLATE_KG = 10_000_000; // 10.000 t in kg

export function formatPesoTotale(n: number): string {
    const kg = n ?? 0;
    if (kg >= SOGLIA_TONNELLATE_KG) {
        const t = (kg / 1000).toLocaleString('it-IT', {maximumFractionDigits: 1});
        return `${t} t`;
    }
    return formatKg(kg);
}

// Controvalore totale in monete d'oro: 100 MR = 10 MA = 1 MO; 1 MP = 10 MO
export function totaleInMo(s: Soldi): number {
    return s.mr / 100 + s.ma / 10 + s.mo + s.mp * 10;
}
