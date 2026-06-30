import {Modificatore} from "./Modificatore";

export interface Attributo {
    id: string;
    label: string;
    modificatore: number;
    percentuale?: number | null;
    modificatori: Modificatore[];
}