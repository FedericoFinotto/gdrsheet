package it.fin8.gdrsheet.dto;

import it.fin8.gdrsheet.def.TipoItem;

import java.util.List;

/**
 * Sostituisce integralmente la configurazione abilitata di un mondo (tipi item e/o liste
 * incantesimi): stessa semantica "full replace" di {@link UpdatePreparedRequest}. Un campo
 * null lascia invariata quella parte di configurazione; una lista vuota disabilita tutto.
 */
public record UpdateMondoConfigRequest(
        List<TipoItem> tipiAbilitati,
        List<String> codiciListeIncantesimi
) {
}
