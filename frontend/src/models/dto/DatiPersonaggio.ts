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
    info?: Record<string, string>;
    // null/assente = PG normale, altrimenti NAVE/NPC/BANCA/STELLA/BASE
    tipoPersonaggio?: string;
    pesoTotale?: number;
    pesoSenzaTaglia?: number;
    pesoMonete?: number;
    tagliaAttuale?: number;
    tagliaBase?: number;
}