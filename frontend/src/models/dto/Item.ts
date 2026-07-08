import {TipoItem} from "../entity/ItemDB";

// Riferimento leggero a un item collegato come figlio (attacchi, forme/trasformazioni di un frutto)
export interface ChildRef {
    id: number;
    nome: string;
    tipo: string;
}

export interface Item {
    id: number;
    nome: string;
    tipo: TipoItem;
    disabled: boolean;
    quantita?: number; // label QTA, default 1
    barriera?: boolean;
    barrMax?: number;
    barrCons?: number;
    utilizziTotale?: number | null;
    utilizziUsati?: number | null;
    scollegabile?: boolean;
    peso?: number | null;
    capienza?: number | null;
    includiArmiAbilitate?: boolean | null;
    includiOggettiAbilitati?: boolean | null;
    includiConsumabiliAbilitati?: boolean | null;
    includiTuttiAbilitati?: boolean | null;
    /** Attacchi (ATTACCO) collegati come figli, precalcolati dal backend. */
    figliAttacchi?: ChildRef[];
}