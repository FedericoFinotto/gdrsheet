package it.fin8.gdrsheet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Applica {@code quantita} milestone ai personaggi selezionati di un gruppo. */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplyMilestoneRequest {
    private List<Integer> personaggioIds;
    private Integer quantita;
}
