import {Nota} from './Quest'

// Nodo di un albero di INFO (radice o sotto-info), specchio di InfoDTO.java. Stessa struttura
// di Quest (vedi Quest.ts) ma senza completamento né "in carico": il contenuto è la lista note.
export interface Info {
    id: number;
    nome: string;
    // Descrizione e note NON arrivano con l'albero (sono la parte pesante): il server le manda
    // solo su /info/{id}/dettaglio, quando l'INFO viene effettivamente aperto.
    descrizione: string | null;
    // già filtrate lato server in base a chi guarda: solo le note effettivamente visibili.
    note: Nota[];
    // solo lato client: true una volta scaricato il dettaglio, per non richiederlo a ogni apertura.
    dettaglioCaricato?: boolean;
    figli: Info[];
    // Solo per gli INFO radice: "PARTY" | "MONDO" | "PERSONAGGIO". Assente/null per i sotto-info.
    ambito: string | null;
    // Solo per gli INFO radice di ambito PERSONAGGIO: nome del personaggio a cui è associato.
    personaggioNome: string | null;
}
