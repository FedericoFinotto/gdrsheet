package it.fin8.gdrsheet.dto;

import it.fin8.gdrsheet.def.CardEditorItem;

import java.util.List;

/**
 * Sostituisce integralmente la configurazione di un (mondo, tipo item): stessa semantica
 * "full replace" di {@link UpdateMondoConfigRequest}. Un campo null lascia invariata quella parte;
 * per {@code campiTitolo}, stringa vuota rimuove il titolo (torna a campi inline senza card). I 4
 * campi *Abilitate/i sono applicati solo se {@code tipo == INCANTESIMO} (ignorati altrimenti).
 */
public record UpdateTipoItemConfigRequest(
        List<CardEditorItem> cardAbilitate,
        String campiTitolo,
        List<TipoItemConfigDTO.CampoLiberoDTO> campiLiberi,
        List<String> scuoleAbilitate,
        List<String> sottoscuoleAbilitate,
        List<String> descrittoriAbilitati,
        List<String> componentiAbilitati
) {
}
