// Entità Sistema
import {TipoItem, TipoModificatore} from "../function/Constants";

export interface Sistema {
    id: number;
    descrizione: string;
}

// Entità Mondo
export interface Mondo {
    id: number;
    descrizione: string;
    idSistema: Sistema;
}

// Interfaccia Personaggio
export interface Personaggio {
    id: number;
    nome: string;
    mondo: Mondo;
    items: ItemDB[];
    stats: StatValue[];
}

// Stub per Stat (da definire secondo la tua entità Stat Java)
export interface Stat {
    id: string;
    tipo: string;
    label: string;
}

// StatValue
export interface StatValue {
    id: number;
    personaggio: Personaggio;
    stat: Stat;
    valore: string;
    mod: Stat;
    classe: boolean;
    addestramento: boolean;
}

// Collegamento (N-ary relation tra items)
export interface Collegamento {
    id: number;
    itemSource: ItemDB;
    itemTarget: ItemDB;
}

// Modificatore
export interface Modificatore {
    id: number;
    item: ItemDB;
    stat: Stat;
    valore: string;
    nota?: string;
    tipo: TipoModificatore;
    sempreAttivo?: boolean;
}

// ItemLabel
export interface ItemLabel {
    id: number;
    item: ItemDB;
    label?: string;
    valore?: string;
}

// Avanzamento
export interface Avanzamento {
    id: number;
    itemSource: ItemDB;
    itemTarget: ItemDB;
    livello: number;
}

// Item (entità principale)
export interface ItemDB {
    id: number;
    nome: string;
    tipo: TipoItem;
    descrizione?: string;
    personaggio?: Personaggio;
    sistema?: Sistema;
    mondo?: Mondo;
    child: Collegamento[];
    modificatori: Modificatore[];
    labels: ItemLabel[];
    avanzamento: Avanzamento[];
}
