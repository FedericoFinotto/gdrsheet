import {defineStore} from 'pinia'
import {getAllPersonaggioItemsDTOByIdPersonaggio, getModificatoriPersonaggioById} from "../service/PersonaggioService";
import type {Statistiche} from "../models/Modificatori";
import type {ItemsPersonaggio} from "../models/Items";

export interface SharedDataState {
    character: any | null;
    modificatori: Statistiche | null;
    items: ItemsPersonaggio | null;
    loading: boolean;
    error: any | null;
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
                this.cache[id] = {character: null, modificatori: null, items: null, loading: true, error: null};
            }

            try {
                // 1) modifiers first
                const modsRes = await getModificatoriPersonaggioById(id);
                this.cache[id].modificatori = modsRes.data;
                // 2) then items
                const itemsRes = await getAllPersonaggioItemsDTOByIdPersonaggio(id);
                this.cache[id].items = itemsRes.data;
            } catch (err) {
                this.cache[id].error = err;
            } finally {
                this.cache[id].loading = false;
            }

            return this.cache[id];
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
