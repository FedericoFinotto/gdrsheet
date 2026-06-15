package it.fin8.gdrsheet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Aggiornamento dei ranghi di più livelli in un'unica transazione (pagina "Gestisci gradi").
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateRanghiBulkRequest {

    private Integer personaggioId;
    private List<LivelloRanghi> livelli;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LivelloRanghi {
        private Integer livelloId;
        private List<UpdateLivelloRequest.RangoSpesoDTO> ranghi;
    }
}
