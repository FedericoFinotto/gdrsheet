// Registro tipo item -> componente editor.
// Usato sia dall'editor (ItemEditor) sia dalla pagina di creazione (ItemCreate).
//
// Con l'introduzione della configurazione per (mondo, tipo item) — vedi MondoAdminService,
// GET /mondo/{id}/tipo-item/{tipo}/config — 32 dei 36 tipi condividono ORA lo stesso componente
// BaseItemEditor.vue: non servono più wrapper "Editor/Tipi/*.vue" per passare CAMPI/titolo/flag,
// perché BaseItemEditor.vue recupera da solo card abilitate e campi liberi in base a mondo+tipo.
// Restano famiglie a parte solo i 3 tipi con forma dati radicalmente diversa: CLASSE/RAZZA
// (ClasseEditor.vue), INCANTESIMO (SpellEditor.vue), LIVELLO (LivelloEditor.vue).
import type {Component} from 'vue'
import {TipoItem, TIPO_ITEM, TIPO_ITEM_LABELS} from '../../../../../../models/entity/ItemDB'

import SpellEditor from './SpellEditor.vue'
import LivelloEditor from './LivelloEditor/LivelloEditor.vue'
import BaseItemEditor from './BaseItemEditor.vue'
import ClasseEditor from './Tipi/ClasseEditor.vue'

export const EDITOR_BY_TYPE: Record<TipoItem, Component> = {
    [TIPO_ITEM.ABILITA]: BaseItemEditor,
    [TIPO_ITEM.TALENTO]: BaseItemEditor,
    [TIPO_ITEM.OGGETTO]: BaseItemEditor,
    [TIPO_ITEM.CONSUMABILE]: BaseItemEditor,
    [TIPO_ITEM.ARMA]: BaseItemEditor,
    [TIPO_ITEM.MUNIZIONE]: BaseItemEditor,
    [TIPO_ITEM.EQUIPAGGIAMENTO]: BaseItemEditor,
    [TIPO_ITEM.PERSONAGGIO]: BaseItemEditor,
    [TIPO_ITEM.CLASSE]: ClasseEditor,
    [TIPO_ITEM.RAZZA]: ClasseEditor,
    [TIPO_ITEM.ATTACCO]: BaseItemEditor,
    [TIPO_ITEM.ALTRO]: BaseItemEditor,
    [TIPO_ITEM.LIVELLO]: LivelloEditor,
    [TIPO_ITEM.MALEDIZIONE]: BaseItemEditor,
    [TIPO_ITEM.INCANTESIMO]: SpellEditor,
    [TIPO_ITEM.TRASFORMAZIONE]: BaseItemEditor,
    [TIPO_ITEM.AVANZAMENTO]: BaseItemEditor,
    [TIPO_ITEM.COMPETENZA]: BaseItemEditor,
    [TIPO_ITEM.LINGUA]: BaseItemEditor,
    [TIPO_ITEM.IDOLO]: BaseItemEditor,
    [TIPO_ITEM.FRUTTO]: BaseItemEditor,
    [TIPO_ITEM.FORMA]: BaseItemEditor,
    [TIPO_ITEM.PRIVILEGIO]: BaseItemEditor,
    [TIPO_ITEM.CONTENITORE]: BaseItemEditor,
    [TIPO_ITEM.PATTO]: BaseItemEditor,
    [TIPO_ITEM.NOTIZIA]: BaseItemEditor,
    [TIPO_ITEM.EFFETTO]: BaseItemEditor,
    [TIPO_ITEM.QUEST]: BaseItemEditor,
    [TIPO_ITEM.VEICOLO]: BaseItemEditor,
    [TIPO_ITEM.INFO]: BaseItemEditor,
    [TIPO_ITEM.CATEGORIA]: BaseItemEditor,
    [TIPO_ITEM.TAG]: BaseItemEditor,
    [TIPO_ITEM.RANDOMIZZATORE]: BaseItemEditor,
    [TIPO_ITEM.CASO]: BaseItemEditor,
    [TIPO_ITEM.SKILL_TRICK]: BaseItemEditor,
    [TIPO_ITEM.IMMAGINE]: BaseItemEditor,
    [TIPO_ITEM.NODO]: BaseItemEditor,
}

// Fallback per eventuali tipi non mappati
export const FALLBACK_EDITOR: Component = BaseItemEditor

export function editorForType(tipo: TipoItem | undefined | null): Component | null {
    if (!tipo) return null
    return EDITOR_BY_TYPE[tipo] ?? FALLBACK_EDITOR
}

// Re-esportata per compatibilità con chi la importava da qui (es. ItemCreate.vue): la
// definizione vive ora in ItemDB.ts, vedi commento lì.
export {TIPO_ITEM_LABELS}

// Tipi creabili dalla pagina di creazione.
// LIVELLO escluso: la creazione di un livello passa dal flusso della scheda
// (LivelloEditor richiede un personaggio associato).
export const CREATABLE_TYPES: TipoItem[] = (Object.values(TIPO_ITEM) as TipoItem[])
    .filter(t => t !== TIPO_ITEM.LIVELLO)
