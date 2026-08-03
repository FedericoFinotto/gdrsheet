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
        /**
         * Punti spesi in Skill Trick a questo livello. Tutti sulla stessa stat
         * {@link it.fin8.gdrsheet.config.Constants#STAT_SKILL_TRICK}, distinti per singolo
         * Skill Trick tramite {@code itemId} (salvato come "nota" del modificatore RANK).
         */
        private List<SkillTrickPuntoDTO> skillTrick;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SkillTrickPuntoDTO {
        /** Id dell'item SKILL_TRICK nel compendio. */
        private Integer itemId;
        private Integer punti;
    }
}
