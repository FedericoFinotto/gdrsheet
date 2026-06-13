// src/function/useChildCreate.ts
// Canale condiviso per il flusso "crea un nuovo item già collegato all'item che sto editando".
// Il padre, prima di navigare verso l'editor di creazione del figlio, salva uno snapshot del
// proprio form (stashDraft). L'editor figlio, una volta salvato, registra l'item creato
// (setCreatedChild). Al ritorno il padre ripristina lo snapshot e aggancia il nuovo figlio.
import {ref, shallowRef} from 'vue'
import {ChildRef} from '../models/dto/UpdateItemRequest'

export interface ParentDraft {
    target: 'children' | 'forme'   // in quale lista del padre va il nuovo figlio
    tipo: string                   // tipo dell'item padre (guardia al ripristino)
    snapshot: any                  // copia piatta del form del padre
}

const draft = shallowRef<ParentDraft | null>(null)
const createdChild = ref<ChildRef | null>(null)

export default function useChildCreate() {
    function stashDraft(d: ParentDraft) {
        draft.value = d
        createdChild.value = null
    }

    function peekDraft(): ParentDraft | null {
        return draft.value
    }

    function takeDraft(): ParentDraft | null {
        const d = draft.value
        draft.value = null
        return d
    }

    function clearDraft() {
        draft.value = null
        createdChild.value = null
    }

    function setCreatedChild(c: ChildRef) {
        createdChild.value = c
    }

    function takeCreatedChild(): ChildRef | null {
        const c = createdChild.value
        createdChild.value = null
        return c
    }

    return {stashDraft, peekDraft, takeDraft, clearDraft, setCreatedChild, takeCreatedChild}
}
