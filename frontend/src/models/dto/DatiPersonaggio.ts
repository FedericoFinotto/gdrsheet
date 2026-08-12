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
    // Tutte le variabili ($id/@id, già con la chiave prefissata "@...") risolvibili in una
    // formula per questo personaggio: contatori item, taglia, livello per classe, caratteristiche
    // — vedi ModificatoriService.costruisciVariabili/CalcoloService.calcola(formula, DatiPersonaggioDTO)
    // sul backend. Il campo arriva già popolato dalla risposta di getModificatoriPersonaggioById
    // (non era dichiarato qui, ma il valore viaggiava comunque: era la LETTURA lato backend a
    // mancare, non l'invio).
    variabili?: Record<string, number>;
}