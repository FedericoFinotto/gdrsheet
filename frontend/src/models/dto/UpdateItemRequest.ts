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

// Attacco figlio (item ATTACCO) gestito inline dall'editor
export interface AttaccoRow {
    id?: number;     // id dell'item ATTACCO esistente; assente per i nuovi
    nome: string;
    tpc?: string;
    tpd?: string;
    tipoDanni?: string;
}

// Riferimento a un item collegato come child
export interface ChildRef {
    id: number;
    nome: string;
    tipo: TipoItem;
    qty?: number | null;
}

export interface UpdateItemRequest {
    nome?: string;
    descrizione?: string;
    tipo?: TipoItem;          // usato solo in creazione
    idPersonaggio?: number;   // solo creazione: aggancia al FromCompendio del personaggio
    idMondo?: number;         // solo creazione: mondo a cui legare l'item
    idSistema?: number;       // solo creazione: sistema a cui legare l'item
    skipFromCompendio?: boolean; // creazione "al volo" di un figlio: non agganciare al FromCompendio
    labels?: LabelRow[];      // stato completo delle labels
    modificatori?: ModificatoreRow[]; // stato completo dei modificatori
    attacchi?: AttaccoRow[];  // stato completo degli attacchi figli
    children?: { id: number; qty?: number | null }[];  // stato completo degli item collegati (non ATTACCO)
}
