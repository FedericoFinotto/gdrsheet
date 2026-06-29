import {TipoItem} from "../entity/ItemDB";

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
}