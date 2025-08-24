// container principale
import {IncantesimoItem} from "./IncantesimoItem";
import {Item} from "./Item";
import {Attacco} from "./Attacco";
import {SpellBook} from "./SpellBook";

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
    livelli: Item[];
    maledizioni: Item[];
    spellbooks: SpellBook[];
    trasformazioni: Item[];
    competenze: Item[];
    lingue: Item[];
    idoli: Item[];
}