// Catalogo delle liste incantesimi (codici SP_*) con etichetta leggibile.
// Usato per la multiselect delle liste nell'editor classe.

export const SPELL_LIST_LABELS: Record<string, string> = {
    SP_BLACKGUARD: 'Cavaliere Nero', SP_STRENGTH: 'Forza', SP_GREED: 'Avarizia',
    SP_CLERIC: 'Chierico', SP_CORRUPTION: 'Corruzione', SP_EVIL: 'Male',
    SP_DRAGON: 'Drago', SP_OCEAN: 'Oceano', SP_FORCE: 'Forza (Dominio)',
    SP_CELERITY: 'Celerità', SP_PLANT: 'Vegetale', SP_LIBERATION: 'Libertà',
    SP_MADNESS: 'Follia', SP_DEATH: 'Morte', SP_ADEPT: 'Adepto', SP_ANIMAL: 'Animale',
    SP_WINTER: 'Inverno', SP_THIRST: 'Sete', SP_RUNE: 'Rune', SP_SPITE: 'Rancore',
    SP_WATER: 'Acqua', SP_DRUID: 'Druido', SP_ASN: 'Assassino', SP_ASSASIN: 'Assassino',
    SP_TEMPTATION: 'Tentazione', SP_WEATHER: 'Meteo', SP_EARTH: 'Terra', SP_MIND: 'Mente',
    SP_DEATHBOUND: 'Vincolo della Morte', SP_KNOWLEDGE: 'Conoscenza',
    SP_SEAFOLK: 'Genti del Mare', SP_FIRE: 'Fuoco', SP_WAR: 'Guerra',
    SP_BEGUILER: 'Mistificatore', SP_PESTILENCE: 'Pestilenza', SP_HEXBLADE: 'Lama Maledetta',
    SP_HEALER: 'Guaritore', SP_CREATION: 'Creazione', SP_CORRUPT: 'Corrotto', SP_DREAM: 'Sogno',
    SP_BARD: 'Bardo', SP_SORCERER: 'Stregone', SP_SUMMER: 'Estate', SP_CHAOS: 'Caos',
    SP_HUNGER: 'Fame', SP_SAND: 'Sabbia', SP_FURY: 'Furia', SP_WARMAGE: 'Mago da Guerra',
    SP_SHUGENJA: 'Shugenja', SP_GOOD: 'Bene', SP_GLORY: 'Gloria', SP_TRAVEL: 'Viaggio',
    SP_PACT: 'Patto', SP_ONEIROMANCY: 'Oniromanzia', SP_MAGIC: 'Magia', SP_WIZARD: 'Mago',
    SP_OOZE: 'Melma', SP_REPOSE: 'Riposo', SP_CITY: 'Città', SP_SKY: 'Cielo', SP_DEMONIC: 'Demoniaco',
    SP_COLD: 'Freddo', SP_DESTRUCTION: 'Distruzione', SP_AIR: 'Aria',
    SP_RANGER: 'Ranger', SP_LUCK: 'Fortuna', SP_SUN: 'Sole', SP_LAW: 'Legge', SP_DOMINATION: 'Dominazione',
    SP_PURIFICATION: 'Purificazione', SP_PALADIN: 'Paladino', SP_PROTECTION: 'Protezione',
    SP_TRICKERY: 'Inganno', SP_ENTROPY: 'Entropia', SP_DESTINY: 'Destino',
    SP_HEALING: 'Guarigione', SP_DUSKBLADE: 'Lama Crepuscolare', SP_BLACKWATER: 'Acquenere',
};

export const SPELL_LIST_CODES = Object.keys(SPELL_LIST_LABELS).sort(
    (a, b) => SPELL_LIST_LABELS[a].localeCompare(SPELL_LIST_LABELS[b])
);

export function spellListLabel(code: string): string {
    return SPELL_LIST_LABELS[code] ?? code;
}
