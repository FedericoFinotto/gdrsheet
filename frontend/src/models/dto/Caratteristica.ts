// Caratteristica base
import {Modificatore} from "./Modificatore";

export interface Caratteristica {
    id: string;
    label: string;
    valore: number;
    modificatore: number;
    modificatorePermanente: number;
    modificatori: Modificatore[];
}