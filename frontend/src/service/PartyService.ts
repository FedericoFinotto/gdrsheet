import api from './api'
import {AxiosResponse} from 'axios'
import {Banca, BancaDetail, Conto, Page, PartyDetail, PartyItem, Soldi} from '../models/dto/Party'

export function getParty(id: number): Promise<AxiosResponse<PartyDetail>> {
    return api.get<PartyDetail>(`/party/${id}`)
}

export interface ItemSearchResult {
    id: number;
    nome: string;
    tipo: string;
    personaggioId: number;
    personaggioNome: string;
    match: string;        // "nome" | "label <KEY>" | "nota"
    disabled: boolean;
}

export function searchPartyItems(partyId: number, q: string): Promise<AxiosResponse<ItemSearchResult[]>> {
    return api.get<ItemSearchResult[]>(`/party/${partyId}/search-items`, {params: {q}})
}

export function searchPersonaggioItems(idPersonaggio: number, q: string): Promise<AxiosResponse<ItemSearchResult[]>> {
    return api.get<ItemSearchResult[]>(`/personaggi/${idPersonaggio}/search-items`, {params: {q}})
}

export interface GruppoDTO {
    id: number;
    nome: string;
    membriIds: number[];
    capogruppoId: number | null;
}

export function getGruppi(partyId: number): Promise<AxiosResponse<GruppoDTO[]>> {
    return api.get<GruppoDTO[]>(`/party/${partyId}/gruppi`)
}

export function createGruppo(partyId: number, nome: string): Promise<AxiosResponse<GruppoDTO>> {
    return api.post<GruppoDTO>(`/party/${partyId}/gruppo`, {nome})
}

export function saveGruppo(
    gruppoId: number, nome: string, membriIds: number[], capogruppoId: number | null
): Promise<AxiosResponse<GruppoDTO>> {
    return api.put<GruppoDTO>(`/party/gruppo/${gruppoId}`, {nome, membriIds, capogruppoId})
}

export function deleteGruppo(gruppoId: number): Promise<AxiosResponse<void>> {
    return api.delete<void>(`/party/gruppo/${gruppoId}`)
}

export interface MilestonePersonaggio {
    id: number;
    nome: string;
    tipoPersonaggio?: string | null;
    milestone: number;
    livello: number;
    saghe: number;
}

export function getMilestoneGruppo(gruppoId: number): Promise<AxiosResponse<MilestonePersonaggio[]>> {
    return api.get<MilestonePersonaggio[]>(`/party/gruppo/${gruppoId}/milestone`)
}

export function applyMilestoneGruppo(gruppoId: number, personaggioIds: number[], quantita: number): Promise<AxiosResponse<MilestonePersonaggio[]>> {
    return api.post<MilestonePersonaggio[]>(`/party/gruppo/${gruppoId}/milestone`, {personaggioIds, quantita})
}

// Saghe necessarie per salire di livello, in base al livello attuale (deve combaciare col backend).
export function saghePerLivello(livello: number): number {
    if (livello <= 10) return 1
    if (livello <= 13) return 3
    if (livello <= 16) return 4
    if (livello <= 18) return 5
    if (livello <= 20) return 6
    if (livello <= 25) return 7
    if (livello <= 30) return 8
    return 8 + Math.ceil((livello - 30) / 5)
}

export function getPartyItems(
    id: number,
    params: { nome?: string; tipo?: string; personaggioId?: number; page?: number; size?: number } = {}
): Promise<AxiosResponse<Page<PartyItem>>> {
    return api.get<Page<PartyItem>>(`/party/${id}/items`, {params})
}

export function giveItem(
    itemId: number, fromPersonaggioId: number, toPersonaggioId: number, toContenitoreId?: number
): Promise<AxiosResponse<void>> {
    return api.post<void>('/party/give', {itemId, fromPersonaggioId, toPersonaggioId, toContenitoreId})
}

// Possibile destinazione di un "Dai a": il personaggio principale (contenitoreId assente),
// oppure uno dei suoi contenitori con inventario separato (es. la Stiva di una NAVE).
export interface Destinatario {
    label: string;
    personaggioId: number;
    contenitoreId?: number;
}

interface DestinatarioGiveDTO {
    personaggioId: number;
    personaggioNome: string;
    contenitoreId: number | null;
    contenitoreNome: string | null;
}

// Destinatari possibili per un item posseduto da idPersonaggio, già calcolati server-side
// (evita di dover conoscere il partyId lato frontend): stessa logica/label della pagina Item del party.
export async function getDestinatariGive(idPersonaggio: number): Promise<AxiosResponse<Destinatario[]>> {
    const res = await api.get<DestinatarioGiveDTO[]>(`/party/personaggio/${idPersonaggio}/destinatari-give`)
    return {
        ...res,
        data: res.data.map(d => ({
            label: d.contenitoreNome ? `${d.personaggioNome} (${d.contenitoreNome})` : d.personaggioNome,
            personaggioId: d.personaggioId,
            contenitoreId: d.contenitoreId ?? undefined,
        })),
    }
}

export function getBanche(partyId: number): Promise<AxiosResponse<Banca[]>> {
    return api.get<Banca[]>(`/party/${partyId}/banche`)
}

export function apriConto(bancaId: number, cc: string): Promise<AxiosResponse<Conto>> {
    return api.post<Conto>(`/party/banca/${bancaId}/conto`, null, {params: {cc}})
}

export function updateConto(itemId: number, soldi: Soldi): Promise<AxiosResponse<Soldi>> {
    return api.post<Soldi>(`/party/banca/conto/${itemId}`, soldi)
}

export function getBancaDetail(bancaId: number): Promise<AxiosResponse<BancaDetail>> {
    return api.get<BancaDetail>(`/party/banca/${bancaId}/dettaglio`)
}

export interface Mondo {
    id: number;
    nome: string;
}

export function getMieiMondi(): Promise<AxiosResponse<Mondo[]>> {
    return api.get<Mondo[]>('/party/mondi')
}

export function createParty(nome: string, mondoId: number): Promise<AxiosResponse<number>> {
    return api.post<number>('/party', {nome, mondoId})
}

export const TIPI_PERSONAGGIO = [
    {value: 'PG', label: 'Personaggio (PG)'},
    {value: 'NPC', label: 'NPC'},
    {value: 'BARCA', label: 'Barca'},
    {value: 'BANCA', label: 'Banca'},
    {value: 'STELLA', label: 'Stella'},
    {value: 'BASE', label: 'Base'},
] as const

export function createPersonaggio(
    partyId: number, nome: string, tipo: string, proprietarioUtenteId?: number
): Promise<AxiosResponse<number>> {
    return api.post<number>('/party/personaggio', {partyId, nome, tipo, proprietarioUtenteId})
}

export function deleteParty(partyId: number): Promise<AxiosResponse<void>> {
    return api.delete<void>(`/party/${partyId}`)
}

export interface MembroParty {
    utenteId: number;
    username: string;
    name: string;
    ruolo: 'MASTER' | 'GIOCATORE';
}

export function getMembri(partyId: number): Promise<AxiosResponse<MembroParty[]>> {
    return api.get<MembroParty[]>(`/party/${partyId}/membri`)
}

export function addMembro(partyId: number, username: string, ruolo: string): Promise<AxiosResponse<MembroParty>> {
    return api.post<MembroParty>(`/party/${partyId}/membro`, {username, ruolo})
}
