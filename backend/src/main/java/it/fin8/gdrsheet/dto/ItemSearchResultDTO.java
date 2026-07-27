package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Risultato di una ricerca "profonda" tra gli item: match su nome, valore di una
 * label o nota di un modificatore. Include il personaggio proprietario e dove ha matchato.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemSearchResultDTO {
    private Integer id;
    private String nome;
    private String tipo;
    private Integer personaggioId;
    private String personaggioNome;
    /** Dove è avvenuto il match: "nome", "descrizione", "label <KEY>", "nota", "nota modificatore". */
    private String match;
    /** Il testo effettivo in cui è stato trovato il match (es. la nota intera), da mostrare come contesto. */
    private String matchTesto;
    private boolean disabled;
}
