export type IconDef =
    | { kind: 'fa'; classes: string }                 // Font Awesome via classi CSS
    | { kind: 'img'; src: string; alt?: string }      // file in /public

// Cambia qui se pubblichi sotto una sottocartella (es. '/scheda/')
const PUBLIC_BASE = '/'; // oppure '/scheda/'
const P = (p: string) =>
    (PUBLIC_BASE.endsWith('/') ? PUBLIC_BASE : PUBLIC_BASE + '/') + p.replace(/^\/+/, '');

export const ICONS = {
    ADD: {kind: 'fa', classes: 'fa-solid fa-plus'},
    SUB: {kind: 'fa', classes: 'fa-solid fa-minus'},
    HOME: {kind: 'fa', classes: 'fa-solid fa-house'},
    HAMBURGER: {kind: 'fa', classes: 'fa-solid fa-bars'},
    COMPENDIO: {kind: 'fa', classes: 'fa-solid fa-book'},
    DADO: {kind: 'fa', classes: 'fa-solid fa-dice-d6'},
    DADO_D20: {kind: 'fa', classes: 'fa-solid fa-dice-d20'},
    SPINNER: {kind: 'fa', classes: 'fa-solid fa-circle-notch'},
    XMARK: {kind: 'fa', classes: 'fa-solid fa-xmark'},
    CHECK: {kind: 'fa', classes: 'fa-solid fa-check'},
    EDIT: {kind: 'fa', classes: 'fa-solid fa-pen-to-square'},
    INFO: {kind: 'fa', classes: 'fa-solid fa-circle-info'},
    DUCK: {kind: 'img', src: P('icons/duck-rubber-debugging.svg'), alt: 'Duck'},
    COMP_V: {kind: 'fa', classes: 'fa-solid fa-v'},    // Verbale
    COMP_S: {kind: 'fa', classes: 'fa-solid fa-s'},         // Somatico
    COMP_M: {kind: 'fa', classes: 'fa-solid fa-M'},         // Materiale
    COMP_F: {kind: 'fa', classes: 'fa-solid fa-wand-sparkles'},      // Focus
    COMP_DF: {kind: 'fa', classes: 'fa-solid fa-wand-sparkles'}, // Focus divino
    COMP_XP: {kind: 'fa', classes: 'fa-solid fa-star'},          // Costo PE
    COMP_X: {kind: 'fa', classes: 'fa-solid fa-coins'},         // Costo in mo (vedi testo)
    COMP_CORRUPT: {kind: 'fa', classes: 'fa-solid fa-skull'},         // Corrupt (BoVD)
    COMP_COLDFIRE: {kind: 'fa', classes: 'fa-solid fa-snowflake'},     // Coldfire (Frostburn) — puoi sostituire con SVG
    COMP_FROSTFELL: {kind: 'fa', classes: 'fa-solid fa-snowflake'},     // Ambiente Frostfell
    COMP_BREATH: {kind: 'fa', classes: 'fa-solid fa-wind'},          // Soffio (B)
    GRADI_DIVINI: {kind: 'fa', classes: 'fa-solid fa-sun'},          // Gradi Divini (party)
    LIVELLO: {kind: 'fa', classes: 'fa-solid fa-gem'},               // Livello atteso (party/info) - "rombo" esperienza
    WARNING: {kind: 'fa', classes: 'fa-solid fa-triangle-exclamation'}, // livello atteso != livelli effettivi
    LEADER: {kind: 'fa', classes: 'fa-solid fa-crown'},              // Capogruppo (party)
    TIPO_STELLA: {kind: 'fa', classes: 'fa-solid fa-star'},          // Tipo personaggio: Stella
    TIPO_NAVE: {kind: 'fa', classes: 'fa-solid fa-ship'},            // Tipo personaggio: Nave/Barca
    TIPO_BASE: {kind: 'fa', classes: 'fa-solid fa-umbrella-beach'},  // Tipo personaggio: Base/Isola
    TIPO_NPC: {kind: 'fa', classes: 'fa-solid fa-user'},             // Tipo personaggio: NPC
    TIPO_BANCA: {kind: 'fa', classes: 'fa-solid fa-building-columns'}, // Tipo personaggio: Banca
} as const satisfies Record<string, IconDef>;

export type IconKey = keyof typeof ICONS;
