// Autenticazione e home utente

export interface UtenteAuth {
    id: number;
    username: string;
    name: string;
    ruolo: string;
}

export interface LoginResponse {
    token: string;
    utente: UtenteAuth;
    mustSetPassword?: boolean;
}

export interface UtenteAdmin {
    id: number;
    username: string;
    name: string;
    ruolo: string;
    mustSetPassword: boolean;
}

export interface PartyHome {
    id: number;
    nome: string;
    ruolo: 'MASTER' | 'GIOCATORE';
}

export interface PersonaggioHome {
    id: number;
    nome: string;
    permesso: 'PROPRIETARIO' | 'VISUALIZZATORE';
    partyId?: number;
    partyNome?: string;
    tipoPersonaggio?: string | null; // NAVE, STELLA, null
}

export interface Home {
    utente: UtenteAuth;
    parties: PartyHome[];
    personaggi: PersonaggioHome[];
}
