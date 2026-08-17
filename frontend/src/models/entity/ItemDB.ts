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
    quantita?: number;
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
    FORMA: 'FORMA',
    PRIVILEGIO: 'PRIVILEGIO',
    CONTENITORE: 'CONTENITORE',
    PATTO: 'PATTO',
    NOTIZIA: 'NOTIZIA',
    EFFETTO: 'EFFETTO',
    QUEST: 'QUEST',
    VEICOLO: 'VEICOLO',
    INFO: 'INFO',
    CATEGORIA: 'CATEGORIA',
    TAG: 'TAG',
    RANDOMIZZATORE: 'RANDOMIZZATORE',
    CASO: 'CASO',
    SKILL_TRICK: 'SKILL_TRICK',
    IMMAGINE: 'IMMAGINE',
} as const;
export type TipoItem = typeof TIPO_ITEM[keyof typeof TIPO_ITEM];

// Etichette leggibili per tipo item (titolo dell'editor, selettore di creazione, ecc.).
// Vive qui (non in editorRegistry.ts) perché BaseItemEditor.vue ne ha bisogno per il proprio
// titolo e importarla da editorRegistry.ts (che importa BaseItemEditor.vue) creerebbe un ciclo.
export const TIPO_ITEM_LABELS: Record<TipoItem, string> = {
    [TIPO_ITEM.ABILITA]: 'Abilità',
    [TIPO_ITEM.TALENTO]: 'Talento',
    [TIPO_ITEM.OGGETTO]: 'Oggetto',
    [TIPO_ITEM.CONSUMABILE]: 'Consumabile',
    [TIPO_ITEM.ARMA]: 'Arma',
    [TIPO_ITEM.MUNIZIONE]: 'Munizione',
    [TIPO_ITEM.EQUIPAGGIAMENTO]: 'Equipaggiamento',
    [TIPO_ITEM.PERSONAGGIO]: 'Personaggio',
    [TIPO_ITEM.CLASSE]: 'Classe',
    [TIPO_ITEM.RAZZA]: 'Razza',
    [TIPO_ITEM.ATTACCO]: 'Attacco',
    [TIPO_ITEM.ALTRO]: 'Altro',
    [TIPO_ITEM.LIVELLO]: 'Livello',
    [TIPO_ITEM.MALEDIZIONE]: 'Maledizione',
    [TIPO_ITEM.INCANTESIMO]: 'Incantesimo',
    [TIPO_ITEM.TRASFORMAZIONE]: 'Trasformazione',
    [TIPO_ITEM.AVANZAMENTO]: 'Avanzamento',
    [TIPO_ITEM.COMPETENZA]: 'Competenza',
    [TIPO_ITEM.LINGUA]: 'Lingua',
    [TIPO_ITEM.IDOLO]: 'Idolo',
    [TIPO_ITEM.FRUTTO]: 'Frutto',
    [TIPO_ITEM.FORMA]: 'Forma',
    [TIPO_ITEM.PRIVILEGIO]: 'Privilegio di Classe',
    [TIPO_ITEM.CONTENITORE]: 'Contenitore',
    [TIPO_ITEM.PATTO]: 'Patto',
    [TIPO_ITEM.NOTIZIA]: 'Notizia',
    [TIPO_ITEM.EFFETTO]: 'Effetto',
    [TIPO_ITEM.QUEST]: 'Quest',
    [TIPO_ITEM.VEICOLO]: 'Veicolo',
    [TIPO_ITEM.INFO]: 'Info',
    [TIPO_ITEM.CATEGORIA]: 'Categoria',
    [TIPO_ITEM.TAG]: 'Tag',
    [TIPO_ITEM.RANDOMIZZATORE]: 'Randomizzatore',
    [TIPO_ITEM.CASO]: 'Caso',
    [TIPO_ITEM.SKILL_TRICK]: 'Skill Trick',
    [TIPO_ITEM.IMMAGINE]: 'Immagine',
};

