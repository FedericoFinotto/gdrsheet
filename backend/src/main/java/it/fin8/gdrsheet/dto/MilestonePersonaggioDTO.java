package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Stato milestone di un personaggio per la livellazione di gruppo:
 * milestone attuali, livello e saghe necessarie per il prossimo livello.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MilestonePersonaggioDTO {
    private Integer id;
    private String nome;
    private String tipoPersonaggio;
    private int milestone;
    private int livello;
    private int saghe;
}
