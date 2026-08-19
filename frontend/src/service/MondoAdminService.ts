import api from './api';
import {AxiosResponse} from 'axios';
import {MondoOpt} from '../function/useMondoSistema';

// Permesso su un mondo: tre valori completamente indipendenti (uno non implica gli altri).
// MASTER = potere pieno (party, utenti, item senza restrizioni). STATS = solo stat_default
// (StatsAdmin.vue). PAGINE = solo configurazione (tipi item/card/campi/cataloghi, MondiAdmin.vue
// "Editor per tipo"). Vedi backend TipoPermessoMondo.
export type PermessoMondo = 'MASTER' | 'STATS' | 'PAGINE';

export const PERMESSO_MONDO_LABELS: Record<PermessoMondo, string> = {
    MASTER: 'Master',
    STATS: 'Statistiche',
    PAGINE: 'Pagine',
};

export interface MasterMondo {
    utenteId: number;
    username: string;
    name: string;
    permesso: PermessoMondo;
}

// Tutti i mondi (admin): per la UI di gestione dei permessi per mondo — SOLO per le azioni
// riservate agli admin (crea mondo/sistema, gestione permessi). Per il selettore mondo della
// pagina, usare getMondiGestibili sotto (scoped a chi ha il permesso PAGINE su un mondo).
export function getMondiAdmin(): Promise<AxiosResponse<MondoOpt[]>> {
    return api.get<MondoOpt[]>('/mondo');
}

// Mondi che l'utente loggato può configurare (tipi item abilitati, editor per tipo, catalogo
// scuole/liste incantesimi): tutti per un admin, solo quelli su cui si ha il permesso PAGINE altrimenti.
export function getMondiGestibili(): Promise<AxiosResponse<MondoOpt[]>> {
    return api.get<MondoOpt[]>('/mondo/gestibili');
}

// Mondi tra cui l'utente loggato può switchare: tutti per un admin, solo quelli di cui è MASTER
// altrimenti. Usato dallo switcher mondo nel menu (mostrato solo se ce ne sono 2+).
export function getMondiDisponibili(): Promise<AxiosResponse<MondoOpt[]>> {
    return api.get<MondoOpt[]>('/mondo/disponibili');
}

// I miei permessi (mondo), indipendentemente dall'admin mode: ho almeno un mondo con MASTER/
// STATS/PAGINE? Usato dal menu (UpperBar.vue) per decidere quali voci mostrare a chi non è (o
// non è in modalità) admin.
export interface MieiPermessiMondo {
    master: boolean;      // MASTER su almeno un mondo qualsiasi (non scoped) — Gestione Utenti
    stats: boolean;       // STATS sul mondo passato (o su almeno un mondo se nessuno passato)
    pagine: boolean;      // PAGINE sul mondo passato (o su almeno un mondo se nessuno passato)
    masterMondo: boolean; // MASTER sul mondo passato (o su almeno un mondo se nessuno passato) — Crea party
}
// mondoId = mondo "corrente" (switcher): master resta sempre "almeno un mondo qualsiasi", stats/
// pagine invece diventano scoped a QUEL mondo se passato — vedi commento lato backend.
export function getMieiPermessiMondo(mondoId?: number | null): Promise<AxiosResponse<MieiPermessiMondo>> {
    return api.get<MieiPermessiMondo>('/mondo/miei-permessi', {params: mondoId != null ? {mondoId} : {}});
}

export function getMasterMondo(mondoId: number): Promise<AxiosResponse<MasterMondo[]>> {
    return api.get<MasterMondo[]>(`/mondo/${mondoId}/master`);
}

export function addMasterMondo(mondoId: number, username: string, permesso: PermessoMondo): Promise<AxiosResponse<MasterMondo>> {
    return api.post<MasterMondo>(`/mondo/${mondoId}/master`, {username, permesso});
}

export function removeMasterMondo(mondoId: number, utenteId: number, permesso: PermessoMondo): Promise<AxiosResponse<void>> {
    return api.delete<void>(`/mondo/${mondoId}/master/${utenteId}`, {params: {permesso}});
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
    // se attivo, il costo in azioni (es. TEMPO_SP degli incantesimi) è mostrato coi glifi del
    // font icone Pathfinder2eActions invece che a testo (vedi function/azioni.ts)
    mostraSimboliAzioni: boolean;
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
    mondoId: number, patch: {
        tipiAbilitati?: string[] | null; codiciListeIncantesimi?: string[] | null
        mostraSimboliAzioni?: boolean | null
    }
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
    // valide solo per tipo INCANTESIMO (liste vuote per gli altri tipi): vedi CatalogoIncantesimo
    scuoleAbilitate: string[];
    sottoscuoleAbilitate: string[];
    descrittoriAbilitati: string[];
    componentiAbilitati: string[];
}

// Cosa mostra l'editor per (mondo, tipo item): card strutturali abilitate + campi liberi. Usata
// da BaseItemEditor.vue/ClasseEditor.vue/SpellEditor.vue/LivelloEditor.vue per decidere cosa
// renderizzare, al posto dei vecchi wrapper "Editor/Tipi/*.vue" con CAMPI hardcoded.
export function getTipoItemConfig(mondoId: number, tipo: string): Promise<AxiosResponse<TipoItemConfig>> {
    return api.get<TipoItemConfig>(`/mondo/${mondoId}/tipo-item/${tipo}/config`);
}

// Sostituisce integralmente la configurazione di un (mondo, tipo item) (admin). I 4 campi
// *Abilitate/i sono applicati dal backend solo se tipo === 'INCANTESIMO' (ignorati altrimenti).
export function aggiornaTipoItemConfig(
    mondoId: number, tipo: string,
    patch: {
        cardAbilitate?: string[] | null; campiTitolo?: string | null; campiLiberi?: CampoLiberoDTO[] | null
        scuoleAbilitate?: string[] | null; sottoscuoleAbilitate?: string[] | null
        descrittoriAbilitati?: string[] | null; componentiAbilitati?: string[] | null
    }
): Promise<AxiosResponse<TipoItemConfig>> {
    return api.put<TipoItemConfig>(`/mondo/${mondoId}/tipo-item/${tipo}/config`, patch);
}

// Catalogo globale (admin) di una delle 4 liste di corredo incantesimo: 'SCUOLA' | 'SOTTOSCUOLA' |
// 'DESCRITTORE' | 'COMPONENTE'. Il valore stesso è già l'etichetta mostrata (nessuna etichetta
// separata, a differenza delle liste/domìni SP_*).
export function getCatalogoIncantesimo(tipo: string): Promise<AxiosResponse<string[]>> {
    return api.get<string[]>(`/mondo/catalogo-incantesimo/${tipo}`);
}

export function creaValoreCatalogoIncantesimo(tipo: string, valore: string): Promise<AxiosResponse<string>> {
    return api.post<string>(`/mondo/catalogo-incantesimo/${tipo}`, {valore});
}
