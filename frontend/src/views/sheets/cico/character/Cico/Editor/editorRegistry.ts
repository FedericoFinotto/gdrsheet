// Registro tipo item -> componente editor.
// Usato sia dall'editor (ItemEditor) sia dalla pagina di creazione (ItemCreate).
import type {Component} from 'vue'
import {TipoItem, TIPO_ITEM} from '../../../../../../models/entity/ItemDB'

import SpellEditor from './SpellEditor.vue'
import LivelloEditor from './LivelloEditor/LivelloEditor.vue'
import BaseItemEditor from './BaseItemEditor.vue'

import AbilitaEditor from './Tipi/AbilitaEditor.vue'
import TalentoEditor from './Tipi/TalentoEditor.vue'
import OggettoEditor from './Tipi/OggettoEditor.vue'
import ConsumabileEditor from './Tipi/ConsumabileEditor.vue'
import ArmaEditor from './Tipi/ArmaEditor.vue'
import MunizioneEditor from './Tipi/MunizioneEditor.vue'
import EquipaggiamentoEditor from './Tipi/EquipaggiamentoEditor.vue'
import PersonaggioEditor from './Tipi/PersonaggioEditor.vue'
import ClasseEditor from './Tipi/ClasseEditor.vue'
import AttaccoEditor from './Tipi/AttaccoEditor.vue'
import AltroEditor from './Tipi/AltroEditor.vue'
import MaledizioneEditor from './Tipi/MaledizioneEditor.vue'
import TrasformazioneEditor from './Tipi/TrasformazioneEditor.vue'
import AvanzamentoEditor from './Tipi/AvanzamentoEditor.vue'
import CompetenzaEditor from './Tipi/CompetenzaEditor.vue'
import LinguaEditor from './Tipi/LinguaEditor.vue'
import IdoloEditor from './Tipi/IdoloEditor.vue'
import FruttoEditor from './Tipi/FruttoEditor.vue'
import FormaEditor from './Tipi/FormaEditor.vue'
import PrivilegioEditor from './Tipi/PrivilegioEditor.vue'
import ContenitoreEditor from './Tipi/ContenitoreEditor.vue'
import NotiziaEditor from './Tipi/NotiziaEditor.vue'

export const EDITOR_BY_TYPE: Record<TipoItem, Component> = {
    [TIPO_ITEM.ABILITA]: AbilitaEditor,
    [TIPO_ITEM.TALENTO]: TalentoEditor,
    [TIPO_ITEM.OGGETTO]: OggettoEditor,
    [TIPO_ITEM.CONSUMABILE]: ConsumabileEditor,
    [TIPO_ITEM.ARMA]: ArmaEditor,
    [TIPO_ITEM.MUNIZIONE]: MunizioneEditor,
    [TIPO_ITEM.EQUIPAGGIAMENTO]: EquipaggiamentoEditor,
    [TIPO_ITEM.PERSONAGGIO]: PersonaggioEditor,
    [TIPO_ITEM.CLASSE]: ClasseEditor,
    [TIPO_ITEM.RAZZA]: ClasseEditor,
    [TIPO_ITEM.ATTACCO]: AttaccoEditor,
    [TIPO_ITEM.ALTRO]: AltroEditor,
    [TIPO_ITEM.LIVELLO]: LivelloEditor,
    [TIPO_ITEM.MALEDIZIONE]: MaledizioneEditor,
    [TIPO_ITEM.INCANTESIMO]: SpellEditor,
    [TIPO_ITEM.TRASFORMAZIONE]: TrasformazioneEditor,
    [TIPO_ITEM.AVANZAMENTO]: AvanzamentoEditor,
    [TIPO_ITEM.COMPETENZA]: CompetenzaEditor,
    [TIPO_ITEM.LINGUA]: LinguaEditor,
    [TIPO_ITEM.IDOLO]: IdoloEditor,
    [TIPO_ITEM.FRUTTO]: FruttoEditor,
    [TIPO_ITEM.FORMA]: FormaEditor,
    [TIPO_ITEM.PRIVILEGIO]: PrivilegioEditor,
    [TIPO_ITEM.CONTENITORE]: ContenitoreEditor,
    [TIPO_ITEM.PATTO]: OggettoEditor,
    [TIPO_ITEM.NOTIZIA]: NotiziaEditor,
}

// Fallback per eventuali tipi non mappati
export const FALLBACK_EDITOR: Component = BaseItemEditor

export function editorForType(tipo: TipoItem | undefined | null): Component | null {
    if (!tipo) return null
    return EDITOR_BY_TYPE[tipo] ?? FALLBACK_EDITOR
}

// Etichette leggibili per il selettore della pagina di creazione
export const TIPO_ITEM_LABELS: Record<TipoItem, string> = {
    [TIPO_ITEM.ABILITA]: 'Abilità',
    [TIPO_ITEM.TALENTO]: 'Talento',
    [TIPO_ITEM.OGGETTO]: 'Oggetto',
    [TIPO_ITEM.CONSUMABILE]: 'Consumabile',
    [TIPO_ITEM.ARMA]: 'Arma',
    [TIPO_ITEM.MUNIZIONE]: 'Munizione',
    [TIPO_ITEM.EQUIPAGGIAMENTO]: 'Equipaggiamento',
    [TIPO_ITEM.PERSONAGGIO]: 'Personaggio',
    [TIPO_ITEM.CLASSE]: 'Classe',
    [TIPO_ITEM.RAZZA]: 'Razza',
    [TIPO_ITEM.ATTACCO]: 'Attacco',
    [TIPO_ITEM.ALTRO]: 'Altro',
    [TIPO_ITEM.LIVELLO]: 'Livello',
    [TIPO_ITEM.MALEDIZIONE]: 'Maledizione',
    [TIPO_ITEM.INCANTESIMO]: 'Incantesimo',
    [TIPO_ITEM.TRASFORMAZIONE]: 'Trasformazione',
    [TIPO_ITEM.AVANZAMENTO]: 'Avanzamento',
    [TIPO_ITEM.COMPETENZA]: 'Competenza',
    [TIPO_ITEM.LINGUA]: 'Lingua',
    [TIPO_ITEM.IDOLO]: 'Idolo',
    [TIPO_ITEM.FRUTTO]: 'Frutto',
    [TIPO_ITEM.FORMA]: 'Forma',
    [TIPO_ITEM.PRIVILEGIO]: 'Privilegio di Classe',
    [TIPO_ITEM.CONTENITORE]: 'Contenitore',
    [TIPO_ITEM.PATTO]: 'Patto',
    [TIPO_ITEM.NOTIZIA]: 'Notizia',
}

// Tipi creabili dalla pagina di creazione.
// LIVELLO escluso: la creazione di un livello passa dal flusso della scheda
// (LivelloEditor richiede un personaggio associato).
export const CREATABLE_TYPES: TipoItem[] = (Object.values(TIPO_ITEM) as TipoItem[])
    .filter(t => t !== TIPO_ITEM.LIVELLO)
