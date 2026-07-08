import {Item} from "./Item";

export interface Trasformazione extends Item {
    gruppo: string;
}

// Gruppo di trasformazioni mutuamente esclusive (stesso "gruppo"). Per le forme di un frutto
// il gruppo è impostato esplicitamente a "FORMA".
export interface GruppoTrasformazioni {
    gruppo: string;
    trasformazioni: Trasformazione[];
}