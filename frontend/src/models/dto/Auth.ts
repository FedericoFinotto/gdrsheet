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
    mondoId?: number | null; // per filtrare la home sul mondo selezionato nello switcher
    mondoNome?: string | null;
}

export interface PersonaggioHome {
    id: number;
    nome: string;
    permesso: 'PROPRIETARIO' | 'VISUALIZZATORE';
    partyId?: number;
    partyNome?: string;
    tipoPersonaggio?: string | null; // NAVE, STELLA, null
    preferito: boolean; // per-utente: mostralo assieme a "I tuoi personaggi" anche se VISUALIZZATORE
    // mondo del party del personaggio; null se personaggio "libero" (senza party) — in quel caso
    // resta sempre visibile a prescindere dal mondo selezionato, non essendoci un mondo da filtrare.
    mondoId?: number | null;
    mondoNome?: string | null;
}

export interface Home {
    utente: UtenteAuth;
    parties: PartyHome[];
    personaggi: PersonaggioHome[];
}
