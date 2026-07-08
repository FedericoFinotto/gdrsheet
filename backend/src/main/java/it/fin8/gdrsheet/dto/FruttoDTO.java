package it.fin8.gdrsheet.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Frutto con le sue forme/trasformazioni figlie già raggruppate ("FORMA" esplicito per le forme,
 * gruppo naturale per le trasformazioni), così il frontend non deve ricalcolarle né fare chiamate
 * di dettaglio separate.
 */
@Getter
@Setter
@NoArgsConstructor
public class FruttoDTO extends ItemDTO {
    private List<GruppoTrasformazioniDTO> trasformazioni;
}
