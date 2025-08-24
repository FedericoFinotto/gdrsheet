// Rank entry dentro `rank.ranks`
import {TipoModificatore} from "../entity/Modificatore";

export interface Rank {
    id: number;
    statId: string;
    valore: number;
    nota: string | null;
    tipo: TipoModificatore;
    sempreAttivo: boolean;
    diClasse: boolean;
    classe: string | null;
    item: string;
    itemId: number;
}