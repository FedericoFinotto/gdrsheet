import {defineStore} from 'pinia'
import {getParty} from '../service/PartyService'
import {PartyDetail} from '../models/dto/Party'

export interface PartyDataState {
    party: PartyDetail | null;
    loading: boolean;
    error: any | null;
}

export interface PartyDataCache {
    [id: number]: PartyDataState;
}

export const usePartyStore = defineStore('party', {
    state: (): { cache: PartyDataCache } => ({
        cache: {}
    }),
    actions: {
        /**
         * Fetch data for a partyId, caching result.
         * @param id Party ID
         * @param reset If true, forces reload even if cached
         */
        async fetchParty(id: number, reset: boolean = false) {
            if (this.cache[id] && !this.cache[id].loading && !reset) {
                return this.cache[id];
            }
            if (!this.cache[id]) {
                this.cache[id] = {party: null, loading: true, error: null};
            } else {
                this.cache[id].loading = true;
                this.cache[id].error = null;
            }

            try {
                const res = await getParty(id);
                this.cache[id].party = res.data;
            } catch (err) {
                this.cache[id].error = err;
                throw err;
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
    }
});
