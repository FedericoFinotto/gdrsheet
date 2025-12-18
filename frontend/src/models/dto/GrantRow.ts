import {Modificatore} from "./Modificatore";
import {ItemDB} from "../entity/ItemDB";

export type GrantRow = {
    id: string
    descrizione: string
    tipo: 'ITEM' | 'MOD'
    livello: number
    raw?: ItemDB | Modificatore
}