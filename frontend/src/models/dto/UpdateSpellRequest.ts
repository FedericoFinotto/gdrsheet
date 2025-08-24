import {ClassLevel} from "./ClassLevel";

export interface UpdateSpellRequest {
    nome?: string; // @Size(max = 100)
    descrizione?: string;
    tempo?: string;
    range?: string;
    durata?: string;
    ts?: string;
    componenti?: string[];
    scuole?: string[];
    subscuole?: string[];
    descrittori?: string[];
    classi?: ClassLevel[];
    labelsPatch?: Record<string, string>;
}