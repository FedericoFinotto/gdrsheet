// Collegamento (N-ary relation tra items)
import {ItemDB} from "./ItemDB";

export interface CollegamentoLabel {
    label: string;
    valore: string;
}

export interface Collegamento {
    id: number;
    itemSource: ItemDB;
    itemTarget: ItemDB;
    qty?: number | null;
    formulaQty?: string | null;
    scelta?: string | null;
    labels?: CollegamentoLabel[];
}