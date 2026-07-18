// Nota testuale (rich text) con una propria visibilità, specchio di NotaDTO.java.
// '' = tutti, 'OWNER' = solo proprietario del personaggio (+master/admin), 'MASTER' = solo master/admin.
export interface Nota {
    testo: string;
    visibilita: string;
}

// Nodo di un albero di quest (radice o sotto-quest), specchio di QuestDTO.java.
export interface Quest {
    id: number;
    nome: string;
    descrizione: string | null;
    // Stato proprio di completamento: significativo solo per le quest foglia (senza figli).
    completata: boolean;
    completati: number;
    totali: number;
    // già filtrate lato server in base a chi guarda: solo le note effettivamente visibili.
    note: Nota[];
    figli: Quest[];
    // Solo per le quest radice: "PARTY" | "MONDO" | "PERSONAGGIO". Assente/null per le sotto-quest.
    ambito: string | null;
    // Solo per le quest radice di ambito PERSONAGGIO: nome del personaggio a cui è associata.
    personaggioNome: string | null;
}
