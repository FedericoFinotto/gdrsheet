package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Un collegamento verso un item EFFETTO, con la condizione sotto cui si applica (es. "Indossato")
 * e il nome dell'item sorgente a cui è collegato (per mostrare "Indossato: Nome Oggetto").
 */
@Getter
@Setter
@AllArgsConstructor
public class EffettoDTO {
    private Integer sourceId;
    private String sourceNome;
    private Integer targetId;
    private String targetNome;
    private String condizione;
    private Boolean disabled;
}
