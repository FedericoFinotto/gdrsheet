package it.fin8.gdrsheet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Nuovo valore nel catalogo globale di una lista di corredo incantesimo (Scuola/Sottoscuola/
 *  Descrittore/Componente) — vedi CatalogoIncantesimo. */
public record CreaValoreCatalogoIncantesimoRequest(
        @NotBlank @Size(max = 100) String valore
) {
}
