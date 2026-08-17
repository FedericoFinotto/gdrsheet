package it.fin8.gdrsheet.dto;

import it.fin8.gdrsheet.def.TipoItem;

import java.util.List;

/**
 * Configurazione di un mondo: cosa è abilitato per quel mondo tra i {@link TipoItem} globali e
 * il catalogo globale di liste/domini incantesimi. Usata dal frontend per filtrare i menu tipo
 * item e la multiselect liste/domini nell'editor classe.
 */
public record MondoConfigDTO(
        List<TipoItem> tipiAbilitati,
        List<ListaIncantesimiDTO> listeIncantesimiAbilitate
) {
    public record ListaIncantesimiDTO(String codice, String etichetta) {
    }
}
