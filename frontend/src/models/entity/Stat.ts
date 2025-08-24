// Stub per Stat (da definire secondo la tua entità Stat Java)
export interface Stat {
    id: string;
    tipo: TipoStat | null;
    label: string;
}

export const TIPO_STAT = {
    CARATTERISTICA: 'CAR',
    ABILITA: 'AB',
    TIRO_SALVEZZA: 'TS',
    PUNTI_FERITA: 'PF',
    ATTRIBUTO: 'ATT',
    CLASSE_ARMATURA: 'CA',
    ATTACCO: 'ATK',
    CONTATORE: 'COUNT',
} as const;

export type TipoStat = typeof TIPO_STAT[keyof typeof TIPO_STAT];