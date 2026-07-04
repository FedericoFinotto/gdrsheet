package it.fin8.gdrsheet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Stato completo di un gruppo: nome, membri (id personaggi) ed eventuale capogruppo.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SaveGruppoRequest {
    private String nome;
    private List<Integer> membriIds;
    private Integer capogruppoId;
}
