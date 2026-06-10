// Richiesta generica di creazione/aggiornamento item
import {TipoItem} from "../entity/ItemDB";
import {TipoModificatore} from "../entity/Modificatore";

export interface LabelRow {
    label: string;
    valore: string;
}

export interface ModificatoreRow {
    id?: number;
    statId: string;
    tipo: TipoModificatore;
    valore: string;
    nota?: string;
    sempreAttivo?: boolean;
}

// Campo "specifico per tipo" mappato su una ItemLabel (es. TPC per gli attacchi)
export interface CampoLabel {
    key: string;
    label: string;
    placeholder?: string;
    textarea?: boolean;
}

export interface UpdateItemRequest {
    nome?: string;
    descrizione?: string;
    tipo?: TipoItem;          // usato solo in creazione
    labels?: LabelRow[];      // stato completo delle labels
    modificatori?: ModificatoreRow[]; // stato completo dei modificatori
}
