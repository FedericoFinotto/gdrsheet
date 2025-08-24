// Avanzamento
import {ItemDB} from "./ItemDB";

export interface Avanzamento {
    id: number;
    itemSource: ItemDB;
    itemTarget: ItemDB;
    livello: number;
}