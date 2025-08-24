// estende Voce per gli incantesimi (hanno cd e livello)
import {Item} from "./Item";

export interface IncantesimoItem extends Item {
    cd: number | null;
    livello: number;
}