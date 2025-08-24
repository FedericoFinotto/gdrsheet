// Interfaccia Personaggio
import {Mondo} from "./Mondo";
import {ItemDB} from "./ItemDB";
import {StatValue} from "./StatValue";

export interface Personaggio {
    id: number;
    nome: string;
    // mondo: Mondo;
    //party
    items: ItemDB[];
    stats: StatValue[];
    //personaggioLabel
}