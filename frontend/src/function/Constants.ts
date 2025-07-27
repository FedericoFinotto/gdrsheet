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
} as const;

export type TipoItem = typeof TIPO_ITEM[keyof typeof TIPO_ITEM];

// Tipo stat (tipo_stat)
export const TIPO_STAT = {
    CAR: 'CAR',
    AB: 'AB',
    TS: 'TS',
    PF: 'PF',
    ATT: 'ATT',
    CA: 'CA',
} as const;

export type TipoStat = typeof TIPO_STAT[keyof typeof TIPO_STAT];

// Tipo permesso (tipo_permesso)
export const TIPO_PERMESSO = {
    R: 'R', // Read
    W: 'W', // Write
} as const;

export type TipoPermesso = typeof TIPO_PERMESSO[keyof typeof TIPO_PERMESSO];

// Tipo ruolo (tipo_ruolo)
export const TIPO_RUOLO = {
    ADMIN: 'ADMIN',
    MASTER: 'MASTER',
    EDITOR: 'EDITOR',
    GIOCATORE: 'GIOCATORE',
} as const;

export type TipoRuolo = typeof TIPO_RUOLO[keyof typeof TIPO_RUOLO];

// Tipo modificatore (tipo_modificatore)
export const TIPO_MODIFICATORE = {
    RANK: 'RANK',
    VALORE: 'VALORE',
    CA_SCHIVARE: 'CA_SCHIVARE',
    CA_ARMOR: 'CA_ARMOR',
    CA_NATURALE: 'CA_NATURALE',
    CA_DEVIAZIONE: 'CA_DEVIAZIONE',
    CA_SHIELD: 'CA_SHIELD',
    CA_MAGIC: 'CA_MAGIC',
} as const;

export type TipoModificatore = typeof TIPO_MODIFICATORE[keyof typeof TIPO_MODIFICATORE];
