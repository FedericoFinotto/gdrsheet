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
        List<String> codiciListeIncantesimi,
        // scalare, non "full replace": null = non toccare, altrimenti sovrascrive direttamente il
        // campo su Mondo (diverso dalle due liste sopra, che sono tabelle di join)
        Boolean mostraSimboliAzioni,
        // Idem: scalari, null = non toccare (vedi Constants.SISTEMA_INCANTESIMI_*/LISTA_INCANTESIMI_*).
        String sistemaIncantesimi,
        String formulaManaIncantesimi,
        String formulaCdIncantesimi,
        String listaIncantesimi,
        Boolean mostraCasterLevel
) {
}
