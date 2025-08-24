import {ClassLevel} from "./ClassLevel";

export interface UpdateLivelloRequest {
    livello: number | null
    caratteristiche: Caratteristiche
    tipoScelta: TipoScelta
    classeId: Id | null
    maledizioneId: Id | null
    // mappa: <abilitaUid string> -> PUNTI SPESI (non ranks)
    ranghi: Record<string, number>
    // mappa livelli di classe
    livelliClasse: Record<number, boolean>
}