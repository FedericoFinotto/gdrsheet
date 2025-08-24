

// Tipo stat (tipo_stat)
export const TIPO_STAT = {
    CAR: 'CAR',
    AB: 'AB',
    TS: 'TS',
    PF: 'PF',
    ATT: 'ATT',
    CA: 'CA',
    ATK: 'ATK',
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


// export const SECONDARY_COLOR = 'rgba(0, 0, 225, 0.35)';
// export const TERTIARY_COLOR = 'rgba(0, 99, 108, 0.25)';
// export const PRIMARY_COLOR = 'rgba(9,50,0,0.25)';

// Palette stile pergamena per l'app DnD
export const PRIMARY_COLOR = 'rgba(255,255,255,0.99)';
export const SECONDARY_COLOR = '#a8ffb4';
export const TERTIARY_COLOR = '#d3fffa';
// Opzionali per ulteriori variazioni
export const ACCENT_COLOR = '#ffdba6';
export const BORDER_COLOR = '#000000';

