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
    DUCK: {kind: 'img', src: P('icons/duck-rubber-debugging.svg'), alt: 'Duck'},
} as const satisfies Record<string, IconDef>;

export type IconKey = keyof typeof ICONS;
