package it.fin8.gdrsheet.dto;

import it.fin8.gdrsheet.def.CardEditorItem;

import java.util.List;

/**
 * Configurazione di un tipo item per un mondo specifico: quali card strutturali sono abilitate
 * (BaseItemEditor.vue per la maggior parte dei tipi; ClasseEditor.vue per CLASSE/RAZZA,
 * SpellEditor.vue per INCANTESIMO, LivelloEditor.vue per LIVELLO — vedi {@link CardEditorItem})
 * e quali campi liberi (ex {@code CAMPI} hardcoded) mostrare.
 */
public record TipoItemConfigDTO(
        List<CardEditorItem> cardAbilitate,
        String campiTitolo,
        List<CampoLiberoDTO> campiLiberi
) {
    public record CampoLiberoDTO(
            String chiave,
            String etichetta,
            String tipoCampo,
            String placeholder,
            boolean textarea,
            boolean multiValore,
            boolean html,
            List<OpzioneDTO> opzioni
    ) {
    }

    public record OpzioneDTO(String value, String label) {
    }
}
