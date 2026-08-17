import api from './api';
import {AxiosResponse} from 'axios';
import {MondoOpt} from '../function/useMondoSistema';

export interface MasterMondo {
    utenteId: number;
    username: string;
    name: string;
}

// Tutti i mondi (admin): per la UI di gestione dei permessi per mondo.
export function getMondiAdmin(): Promise<AxiosResponse<MondoOpt[]>> {
    return api.get<MondoOpt[]>('/mondo');
}

// Mondi tra cui l'utente loggato può switchare: tutti per un admin, solo quelli di cui è master
// altrimenti. Usato dallo switcher mondo nel menu (mostrato solo se ce ne sono 2+).
export function getMondiDisponibili(): Promise<AxiosResponse<MondoOpt[]>> {
    return api.get<MondoOpt[]>('/mondo/disponibili');
}

export function getMasterMondo(mondoId: number): Promise<AxiosResponse<MasterMondo[]>> {
    return api.get<MasterMondo[]>(`/mondo/${mondoId}/master`);
}

export function addMasterMondo(mondoId: number, username: string): Promise<AxiosResponse<MasterMondo>> {
    return api.post<MasterMondo>(`/mondo/${mondoId}/master`, {username});
}

export function removeMasterMondo(mondoId: number, utenteId: number): Promise<AxiosResponse<void>> {
    return api.delete<void>(`/mondo/${mondoId}/master/${utenteId}`);
}

// Sistemi (admin): lista + creazione.
export function getSistemiAdmin(): Promise<AxiosResponse<MondoOpt[]>> {
    return api.get<MondoOpt[]>('/mondo/sistemi');
}

export function creaSistema(descrizione: string): Promise<AxiosResponse<MondoOpt>> {
    return api.post<MondoOpt>('/mondo/sistemi', {descrizione});
}

// Mondi (admin): creazione (sistemaId obbligatorio) + aggiornamento (rinomina/riassegna sistema).
export function creaMondo(descrizione: string, sistemaId: number): Promise<AxiosResponse<MondoOpt>> {
    return api.post<MondoOpt>('/mondo', {descrizione, sistemaId});
}

export function aggiornaMondo(
    mondoId: number, patch: { descrizione?: string; sistemaId?: number }
): Promise<AxiosResponse<MondoOpt>> {
    return api.put<MondoOpt>(`/mondo/${mondoId}`, patch);
}

export interface ListaIncantesimiOpt {
    codice: string;
    etichetta: string;
}

export interface MondoConfig {
    tipiAbilitati: string[];
    listeIncantesimiAbilitate: ListaIncantesimiOpt[];
}

// Cosa è abilitato per questo mondo (tipi item + liste/domini incantesimi): usato per filtrare i
// menu di creazione/editing invece del vecchio catalogo statico globale.
export function getConfigMondo(mondoId: number): Promise<AxiosResponse<MondoConfig>> {
    return api.get<MondoConfig>(`/mondo/${mondoId}/config`);
}

// Catalogo globale delle liste/domini incantesimi (admin): per la UI di gestione che decide cosa
// abilitare in un mondo.
export function getCatalogoListeIncantesimi(): Promise<AxiosResponse<ListaIncantesimiOpt[]>> {
    return api.get<ListaIncantesimiOpt[]>('/mondo/liste-incantesimi');
}

// Aggiunge un nuovo codice al catalogo globale (admin): non lo abilita automaticamente per
// nessun mondo, va poi abilitato esplicitamente in "Liste / domini incantesimi abilitati".
export function creaListaIncantesimi(codice: string, etichetta: string): Promise<AxiosResponse<ListaIncantesimiOpt>> {
    return api.post<ListaIncantesimiOpt>('/mondo/liste-incantesimi', {codice, etichetta});
}

// Sostituisce integralmente tipi/liste abilitati di un mondo (admin). Un campo omesso/null
// lascia invariata quella parte, un array vuoto disabilita tutto.
export function aggiornaConfigMondo(
    mondoId: number, patch: { tipiAbilitati?: string[] | null; codiciListeIncantesimi?: string[] | null }
): Promise<AxiosResponse<MondoConfig>> {
    return api.put<MondoConfig>(`/mondo/${mondoId}/config`, patch);
}

export interface CampoLiberoOpt {
    value: string;
    label: string;
}

export interface CampoLiberoDTO {
    chiave: string;
    etichetta: string;
    tipoCampo: 'TESTO' | 'TEXTAREA' | 'CHECKBOX' | 'SELECT' | 'DATETIME' | null;
    placeholder: string | null;
    textarea: boolean;
    multiValore: boolean;
    html: boolean;
    opzioni: CampoLiberoOpt[];
}

export interface TipoItemConfig {
    cardAbilitate: string[];
    campiTitolo: string | null;
    campiLiberi: CampoLiberoDTO[];
}

// Cosa mostra l'editor per (mondo, tipo item): card strutturali abilitate + campi liberi. Usata
// da BaseItemEditor.vue/ClasseEditor.vue/SpellEditor.vue/LivelloEditor.vue per decidere cosa
// renderizzare, al posto dei vecchi wrapper "Editor/Tipi/*.vue" con CAMPI hardcoded.
export function getTipoItemConfig(mondoId: number, tipo: string): Promise<AxiosResponse<TipoItemConfig>> {
    return api.get<TipoItemConfig>(`/mondo/${mondoId}/tipo-item/${tipo}/config`);
}

// Sostituisce integralmente la configurazione di un (mondo, tipo item) (admin).
export function aggiornaTipoItemConfig(
    mondoId: number, tipo: string,
    patch: { cardAbilitate?: string[] | null; campiTitolo?: string | null; campiLiberi?: CampoLiberoDTO[] | null }
): Promise<AxiosResponse<TipoItemConfig>> {
    return api.put<TipoItemConfig>(`/mondo/${mondoId}/tipo-item/${tipo}/config`, patch);
}
