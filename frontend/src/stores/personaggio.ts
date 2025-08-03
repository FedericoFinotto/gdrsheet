import {defineStore} from 'pinia'
import {getAllPersonaggioItemsDTOByIdPersonaggio, getModificatoriPersonaggioById} from "../service/PersonaggioService";
import {Statistiche} from "../models/Modificatori";
import {ItemsPersonaggio} from "../models/Items";


export interface SharedDataState {
    character: any | null
    modificatori: Statistiche | null
    items: ItemsPersonaggio | null
    loading: boolean
    error: any | null
}

export interface SharedDataCache {
    [id: number]: SharedDataState
}

export const useCharacterStore = defineStore('character', {
    state: (): { cache: SharedDataCache } => ({
        cache: {}
    }),
    actions: {
        /**
         * Fetch data for a characterId, caching result.
         * 1) First fetch modifiers
         * 2) Then in parallel fetch character and items
         */
        async fetchCharacter(id: number, reset: boolean = false) {
            if (this.cache[id] && !this.cache[id].loading && !reset) {
                console.log('DATI PERSONAGGIO LETTI DALLA CACHE');
                return this.cache[id]
            }
            // initialize placeholder
            console.log('RICALCOLO DATI PERSONAGGIO');
            this.cache[id] = {character: null, modificatori: null, items: null, loading: true, error: null}

            try {
                // 1) modifiers first
                getModificatoriPersonaggioById(id).then(resp => {
                    this.cache[id].modificatori = resp.data
                    getAllPersonaggioItemsDTOByIdPersonaggio(id).then(resp => {
                        this.cache[id].items = resp.data
                    })
                })

            } catch (err) {
                this.cache[id].error = err
            } finally {
                this.cache[id].loading = false
            }

            return this.cache[id]
        },

        /**
         * Invalidate cache (force reload on next fetch)
         */
        invalidate(id: number) {
            delete this.cache[id]
        },

        /**
         * Update character on server and refresh cache for that part
         */
        // async updateCharacter(id: number, payload: any) {
        //     await updatePersonaggioById(id, payload)
        //     // refresh only character part
        //     if (!this.cache[id]) this.initialize(id)
        //     const charRes = await getPersonaggioById(id)
        //     this.cache[id].character = charRes.data
        // },

        /**
         * Update modifiers then refresh modifiers in cache
         */
        // async updateModificatori(id: number, payload: any[]) {
        //     await updateModificatoriByCharacterId(id, payload)
        //     if (!this.cache[id]) this.initialize(id)
        //     const modsRes = await getModificatoriPersonaggioById(id)
        //     this.cache[id].modificatori = modsRes.data
        // },

        /**
         * Update items then refresh items in cache
         */
        // async updateItems(id: number, payload: any[]) {
        //     await updateItemsByCharacterId(id, payload)
        //     if (!this.cache[id]) this.initialize(id)
        //     const itemsRes = await getItemsByCharacterId(id)
        //     this.cache[id].items = itemsRes.data
        // },

        /**
         * Helper: initialize empty entry
         */
        initialize(id: number) {
            this.cache[id] = {character: null, modificatori: null, items: null, loading: false, error: null}
        }
    }
})

/* Usage in a component:

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { useCharacterStore } from '@/stores/characterStore'
import { storeToRefs } from 'pinia'

const route = useRoute()
const id = Number(route.params.id)
const characterStore = useCharacterStore()
const { cache } = storeToRefs(characterStore)

// on mount or watch id
await characterStore.fetchCharacter(id)

// in template: cache[id].character, cache[id].modificatori, cache[id].items
</script>
*/
