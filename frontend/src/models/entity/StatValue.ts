// StatValue
import {Personaggio} from "./Personaggio";
import {Stat} from "./Stat";

export interface StatValue {
    id: number;
    personaggio: Personaggio;
    stat: Stat;
    valore: string;
    mod: Stat;
    classe: boolean;
    addestramento: boolean;
}