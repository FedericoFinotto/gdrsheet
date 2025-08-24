import {Modificatore} from "./Modificatore";

export interface Attributo {
    id: string;
    label: string;
    modificatore: number;
    modificatori: Modificatore[];
}