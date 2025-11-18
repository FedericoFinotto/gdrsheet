// container principale
import {Item} from "./Item";
import {Attacco} from "./Attacco";
import {SpellBook} from "./SpellBook";
import {Livello} from "./Livello";
import {Trasformazione} from "./Trasformazione";

export interface Items {
    abilita: Item[];
    talenti: Item[];
    oggetti: Item[];
    consumabili: Item[];
    armi: Item[];
    munizioni: Item[];
    equipaggiamento: Item[];
    classi: Item[];
    razze: Item[];
    attacchi: Attacco[];
    livelli: Livello[];
    maledizioni: Item[];
    spellbooks: SpellBook[];
    trasformazioni: Trasformazione[];
    competenze: Item[];
    lingue: Item[];
    idoli: Item[];
}