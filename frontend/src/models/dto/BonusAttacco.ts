import {Modificatore} from "./Modificatore";

export interface BonusAttacco {
    id: string;
    label: string;
    modificatore: number;
    attacchiMultipli: number[];
    modificatori: Modificatore[];
}