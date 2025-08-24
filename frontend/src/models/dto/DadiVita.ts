// Caratteristica base
import {Modificatore} from "./Modificatore";

export interface DadiVita {
    id: string;
    label: string;
    totale: number;
    totaleStringa: string;
    modificatori: Modificatore[];
}