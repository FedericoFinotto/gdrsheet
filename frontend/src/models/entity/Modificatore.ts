// Modificatore
import {Stat} from "./Stat";
import {ItemDB} from "./ItemDB";


export interface Modificatore {
    id: number;
    item: ItemDB;
    stat: Stat;
    valore: string;
    nota?: string;
    tipo: TipoModificatore;
    sempreAttivo?: boolean;
}

export const TIPO_MODIFICATORE = {
    RANK: 'RANK',
    VALORE: 'VALORE',
    MOD: 'MOD',
    CA_DEVIAZIONE: 'CA_DEVIAZIONE',
    CA_SCHIVARE: 'CA_SCHIVARE',
    CA_ARMOR: 'CA_ARMOR',
    CA_SHIELD: 'CA_SHIELD',
    CA_MAGIC: 'CA_MAGIC',
    CA_NATURALE: 'CA_NATURALE',
    BASE: 'BASE',
} as const;

export type TipoModificatore = typeof TIPO_MODIFICATORE[keyof typeof TIPO_MODIFICATORE];