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
    HAMBURGER: {kind: 'fa', classes: 'fa-solid fa-bars'},
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
} as const satisfies Record<string, IconDef>;

export type IconKey = keyof typeof ICONS;
