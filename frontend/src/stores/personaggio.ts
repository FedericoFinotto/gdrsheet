import {defineStore} from 'pinia'
import {getAllPersonaggioItemsDTOByIdPersonaggio, getModificatoriPersonaggioById} from "../service/PersonaggioService";
import {DatiPersonaggio} from "../models/dto/DatiPersonaggio";
import {Items} from "../models/dto/Items";
import {AttaccoCalcolatoRow, calcolaAttacchi} from "../function/Calcolo";

export interface SharedDataState {
    character: any | null;
    modificatori: DatiPersonaggio | null;
    items: Items | null;
    loading: boolean;
    error: any | null;
    attacchi: AttaccoCalcolatoRow[] | null;
    attacchiLoading: boolean;
}

export interface SharedDataCache {
    [id: number]: SharedDataState;
}

export const useCharacterStore = defineStore('character', {
    state: (): { cache: SharedDataCache } => ({
        cache: {}
    }),
    actions: {
        /**
         * Fetch data for a characterId, caching result.
         * @param id Character ID
         * @param reset If true, forces reload even if cached
         */
        async fetchCharacter(id: number, reset: boolean = false) {
            if (this.cache[id] && !this.cache[id].loading && !reset) {
                console.log('DATI PERSONAGGIO LETTI DALLA CACHE');
                return this.cache[id];
            }
            console.log('RICALCOLO DATI PERSONAGGIO');
            if (!this.cache[id]) {
                this.cache[id] = {character: null, modificatori: null, items: null, loading: true, error: null, attacchi: null, attacchiLoading: false};
            }

            try {
                // 1) modifiers first
                const modsRes = await getModificatoriPersonaggioById(id);
                this.cache[id].modificatori = modsRes.data;
                // 2) then items
                const itemsRes = await getAllPersonaggioItemsDTOByIdPersonaggio(id);
                this.cache[id].items = itemsRes.data;
                // 3) precalcola le formule degli attacchi in background (non attende il risultato):
                // così la pagina Attacchi trova già la cache pronta invece di rifare N chiamate al primo render.
                this.precalcolaAttacchi(id);
            } catch (err) {
                this.cache[id].error = err;
            } finally {
                this.cache[id].loading = false;
            }

            return this.cache[id];
        },

        /**
         * Precalcola in background atk/dmg di tutti gli attacchi del personaggio e li mette in cache.
         */
        async precalcolaAttacchi(id: number) {
            const state = this.cache[id];
            if (!state?.items || !state?.modificatori) return;
            state.attacchiLoading = true;
            try {
                state.attacchi = await calcolaAttacchi(state.items, state.modificatori);
            } catch (err) {
                console.error('Errore precalcolo formule attacchi:', err);
            } finally {
                state.attacchiLoading = false;
            }
        },

        /**
         * Invalidate cache (force reload on next fetch)
         */
        invalidate(id: number) {
            delete this.cache[id];
        },

        /**
         * Force refresh: clear cache and fetch anew.
         */
        // async refreshCharacter(id: number) {
        //     this.invalidate(id);
        //     return this.fetchCharacter(id, true);
        // }
    }
});
