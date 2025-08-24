// Tiro salvezza
import {Modificatore} from "./Modificatore";

export interface TiroSalvezza {
    id: string;
    label: string;
    modificatore: number;
    modificatori: Modificatore[];
}