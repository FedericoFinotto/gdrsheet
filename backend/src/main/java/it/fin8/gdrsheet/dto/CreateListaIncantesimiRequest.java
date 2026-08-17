package it.fin8.gdrsheet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Nuovo codice nel catalogo globale delle liste/domini incantesimi (vedi ListaIncantesimi). */
public record CreateListaIncantesimiRequest(
        @NotBlank @Size(max = 50) String codice,
        @NotBlank @Size(max = 100) String etichetta
) {
}
