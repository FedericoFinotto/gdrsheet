import {TipoModificatore} from "../entity/Modificatore";
import {TipoItem} from "../entity/ItemDB";

export interface Modificatore {
    id: number | null;
    statId: string;
    valore: number;
    formula: string | null;
    nota: string | null;
    tipo: TipoModificatore | null;
    permanente: boolean;
    item: string | null;
    tipoItem: TipoItem | null;
}

