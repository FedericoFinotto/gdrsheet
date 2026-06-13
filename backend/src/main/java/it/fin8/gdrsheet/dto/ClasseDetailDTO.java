package it.fin8.gdrsheet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Rappresentazione completa di una CLASSE per l'editor:
 * dati base + label di classe + tabella dei 20 livelli (item AVANZAMENTO
 * "NOME N" con modificatori BAB/TMP/RFL/VLT/DV/GRADI e label SP_SLOT/SPELL)
 * + abilità concesse per livello (righe avanzamento verso item non-AVANZAMENTO).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClasseDetailDTO {
    private Integer id;            // null in creazione
    private String nome;
    private String descrizione;

    /**
     * Stat id delle abilità di classe (label ABCLASSE, comma-separated).
     */
    private List<String> abilitaClasse;

    /**
     * Lista incantesimi (label SPELL, es. SP_DRUID). Vuoto = non incantatore.
     */
    private String spellList;

    /**
     * Formula slot bonus (label SP_SLOT_BONUS).
     */
    private String spellSlotBonus;

    private List<LivelloClasseDTO> livelli;

    private List<AbilitaConcessaDTO> abilitaConcesse;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LivelloClasseDTO {
        private int livello;       // 1..20
        private String bab;        // es. "+3"
        private String tmp;        // tempra
        private String rfl;        // riflessi
        private String vlt;        // volontà
        private String dv;         // es. "3d8"
        private String gradi;      // es. "6*(@INT+4)"
        private String spSlot;     // es. "4,2,1,0,0,0,0,0,0,0" (vuoto = non incantatore)
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AbilitaConcessaDTO {
        private int livello;
        private Integer itemId;
        private String nome;       // solo in lettura
        private String tipo;       // solo in lettura
    }
}
