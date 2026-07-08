// container principale
import {Item} from "./Item";
import {Attacco} from "./Attacco";
import {SpellBook} from "./SpellBook";
import {Livello} from "./Livello";
import {GruppoTrasformazioni} from "./Trasformazione";

// Frutto con le sue forme/trasformazioni figlie già raggruppate dal backend
// (gruppo "FORMA" esplicito per le forme, gruppo naturale per le trasformazioni).
export interface Frutto extends Item {
    trasformazioni: GruppoTrasformazioni[];
}

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
    // Trasformazioni INDIPENDENTI (non figlie di alcun frutto), già raggruppate per "gruppo".
    trasformazioni: GruppoTrasformazioni[];
    competenze: Item[];
    lingue: Item[];
    idoli: Item[];
    frutti: Frutto[];
    forme: Item[];
    privilegi: Item[];
    contenitori: Item[];
    altro: Item[];
    notizie: Item[];
    patti: Item[];
}