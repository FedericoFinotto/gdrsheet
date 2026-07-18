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
    /** Manuale di provenienza (label MANUALE_SP). */
    manuale?: string;
    /** Descrittori Abilità (label DESCR_STR/DESCR_MAG/DESCR_SOP/DESCR_NAT), possono essere true insieme. */
    descrStraordinaria?: boolean;
    descrMagica?: boolean;
    descrSoprannaturale?: boolean;
    descrNaturale?: boolean;
    descrDivina?: boolean;
    /** Descrittori Oggetto (label MAGICO/PSIONICO/DIVINO/LEGGENDARIO/UNICO), possono essere true insieme. */
    magico?: boolean;
    psionico?: boolean;
    divino?: boolean;
    leggendario?: boolean;
    unico?: boolean;
}