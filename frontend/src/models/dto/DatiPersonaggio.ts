import {ClasseArmatura} from "./ClasseArmatura";
import {Caratteristica} from "./Caratteristica";
import {TiroSalvezza} from "./TiroSalvezza";
import {DadiVita} from "./DadiVita";
import {Abilita} from "./Abilita";
import {Contatore} from "./Contatore";
import {Attributo} from "./Attributo";
import {BonusAttacco} from "./BonusAttacco";

export interface DatiPersonaggio {
    id: number;
    nome: string;
    caratteristiche: Caratteristica[];
    tiriSalvezza: TiroSalvezza[];
    abilita: Abilita[];
    classeArmatura: ClasseArmatura[];
    bonusAttacco: BonusAttacco[];
    contatori: Contatore[];
    attributi: Attributo[];
    dadiVita: DadiVita
}