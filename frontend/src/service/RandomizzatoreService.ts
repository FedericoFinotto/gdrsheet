import api from './api';
import {AxiosResponse} from 'axios';

/** Riferimento leggero a un item (categoria, tag, oggetto estratto). */
export interface Ref {
    id: number;
    nome: string;
}

export interface Categoria {
    id: number;
    nome: string;
    tag: Ref[];
}

/** Riga di associazione (tag, peso) su un oggetto. La categoria deriva dal tag. */
export interface ItemTag {
    idTag: number | null;
    tag?: string;
    idCategoria?: number | null;
    categoria?: string;
    peso: number | null;
}

export interface RandomizzatoreConfig {
    id: number;
    nome: string;
    categorieScelte: Categoria[];
    categoriePresenti: Categoria[];
    combina: 'PRODOTTO' | 'SOMMA';
}

export interface TagEstratto {
    idCategoria: number;
    categoria: string;
    idTag: number;
    tag: string;
    /** true se sorteggiato d'ufficio (non scelto né ereditato dal livello superiore) */
    automatico?: boolean | null;
}

/** Albero: un oggetto estratto può innescare altri randomizzatori (label RAND_TRIGGER). */
export interface Esito {
    idItem?: number | null;
    nome?: string | null;
    descrizione?: string | null;
    scelti: TagEstratto[];
    estratti: TagEstratto[];
    candidati: number;
    eseguitoIl: string;
    idRandomizzatore?: number | null;
    randomizzatore?: string | null;
    figli?: Esito[] | null;
    /** ramo non risolto (nessun candidato, limiti di catena raggiunti…) */
    avviso?: string | null;
}

export interface StoricoVoce {
    id: number;
    eseguitoIl: string;
    utente?: string | null;
    esito?: Esito | null;
}

/** Tutte le categorie con i loro tag (tendine di editor tag ed editor randomizzatore). */
export function getCategorie(): Promise<AxiosResponse<Categoria[]>> {
    return api.get<Categoria[]>('/randomizzatore/categorie');
}

/** Elenco dei randomizzatori, per scegliere quali innescare da un oggetto. */
export function getElencoRandomizzatori(): Promise<AxiosResponse<Ref[]>> {
    return api.get<Ref[]>('/randomizzatore/elenco');
}

export function getTagsItem(idItem: number): Promise<AxiosResponse<ItemTag[]>> {
    return api.get<ItemTag[]>(`/randomizzatore/item/${idItem}/tag`);
}

/** Stato completo: sostituisce integralmente i tag dell'oggetto. */
export function setTagsItem(idItem: number, tags: ItemTag[]): Promise<AxiosResponse<ItemTag[]>> {
    return api.post<ItemTag[]>(`/randomizzatore/item/${idItem}/tag`, {tags});
}

export function getConfig(idRandomizzatore: number): Promise<AxiosResponse<RandomizzatoreConfig>> {
    return api.get<RandomizzatoreConfig>(`/randomizzatore/${idRandomizzatore}/config`);
}

export function estrai(
    idRandomizzatore: number,
    tagScelti: number[],
    idMondo?: number | null,
    idSistema?: number | null,
): Promise<AxiosResponse<Esito>> {
    return api.post<Esito>(`/randomizzatore/${idRandomizzatore}/estrai`, {tagScelti, idMondo, idSistema});
}

export function getStorico(idRandomizzatore: number, limite = 5): Promise<AxiosResponse<StoricoVoce[]>> {
    return api.get<StoricoVoce[]>(`/randomizzatore/${idRandomizzatore}/storico`, {params: {limite}});
}
