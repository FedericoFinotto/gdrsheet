// Entità Sistema
import {Sistema} from "./Sistema";
import {Mondo} from "./Mondo";
import {Collegamento} from "./Collegamento";
import {Modificatore} from "./Modificatore";
import {ItemLabel} from "./ItemLabel";
import {Avanzamento} from "./Avanzamento";

// Item (entità principale)
export interface ItemDB {
    id: number;
    nome: string;
    tipo: TipoItem;
    descrizione?: string;
    sistema?: Sistema;
    mondo?: Mondo;
    child: Collegamento[];
    modificatori: Modificatore[];
    labels: ItemLabel[];
    avanzamento: Avanzamento[];
}

// Tipo item (tipo_item)
export const TIPO_ITEM = {
    ABILITA: 'ABILITA',
    TALENTO: 'TALENTO',
    OGGETTO: 'OGGETTO',
    CONSUMABILE: 'CONSUMABILE',
    ARMA: 'ARMA',
    MUNIZIONE: 'MUNIZIONE',
    EQUIPAGGIAMENTO: 'EQUIPAGGIAMENTO',
    PERSONAGGIO: 'PERSONAGGIO',
    CLASSE: 'CLASSE',
    RAZZA: 'RAZZA',
    ATTACCO: 'ATTACCO',
    ALTRO: 'ALTRO',
    LIVELLO: 'LIVELLO',
    MALEDIZIONE: 'MALEDIZIONE',
    INCANTESIMO: 'INCANTESIMO',
    TRASFORMAZIONE: 'TRASFORMAZIONE',
    AVANZAMENTO: 'AVANZAMENTO',
    COMPETENZA: 'COMP',
    LINGUA: 'LINGUA',
    IDOLO: 'IDOLO',
    FRUTTO: 'FRUTTO',
} as const;
export type TipoItem = typeof TIPO_ITEM[keyof typeof TIPO_ITEM];

