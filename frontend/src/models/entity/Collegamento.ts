// Collegamento (N-ary relation tra items)
import {ItemDB} from "./ItemDB";

export interface Collegamento {
    id: number;
    itemSource: ItemDB;
    itemTarget: ItemDB;
}