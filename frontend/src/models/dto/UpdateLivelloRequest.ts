// Payload effettivamente inviato a POST /item/editlivello/{id}
export interface SaveLivelloPayload {
    livelloId: number
    personaggioId: number
    livello: number | null
    caratteristiche: Record<string, number>
    classeId: number | null
    maledizioneNome: string | null
    dv: string | null
    livelliClasse: number[]
    ranghi: Array<{ abilitaId: string; punti: number }>
    grantsSelezionati: Array<{ id: string; tipo: 'ITEM' | 'MOD'; livello: number; descrizione: string }>
    modificatoriLiberi: Array<{ id?: number; statId: string; tipo: string; valore: string; nota: string; sempreAttivo: boolean }>
}

export interface UpdateLivelloRequest {
    livello: number | null
    caratteristiche: Caratteristiche
    tipoScelta: TipoScelta
    classeId: Id | null
    maledizioneId: Id | null
    maledizioneNome: string | null
    dv: string | null
    // mappa: <abilitaUid string> -> PUNTI SPESI (non ranks)
    ranghi: Record<string, number>
    // mappa livelli di classe
    livelliClasse: Record<number, boolean>
}