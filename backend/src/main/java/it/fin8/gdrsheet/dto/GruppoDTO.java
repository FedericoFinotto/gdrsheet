package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Gruppo di personaggi in un party, con gli id dei membri e l'eventuale capogruppo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GruppoDTO {
    private Integer id;
    private String nome;
    private List<Integer> membriIds;
    private Integer capogruppoId;
}
