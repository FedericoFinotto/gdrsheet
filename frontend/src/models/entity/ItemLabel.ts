// ItemLabel
import {ItemDB} from "./ItemDB";

export interface ItemLabel {
    id: number;
    item: ItemDB;
    label?: string;
    valore?: string;
}