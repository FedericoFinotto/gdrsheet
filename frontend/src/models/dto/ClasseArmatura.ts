// Classe armatura
import {Modificatore} from "./Modificatore";

export interface ClasseArmatura {
    id: string;
    label: string;
    modificatore: number;
    modificatori: Modificatore[];
}