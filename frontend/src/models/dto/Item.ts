import {TipoItem} from "../entity/ItemDB";

export interface Item {
    id: number;
    nome: string;
    tipo: TipoItem;
    disabled: boolean;
}