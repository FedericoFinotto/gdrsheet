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
    private String tipo;           // CLASSE | RAZZA (gestiti con lo stesso editor)
    private String nome;
    private String enName;         // nome originale in inglese (label EN_NAME)
    private String manuale;        // manuale di provenienza (label MANUALE_SP)
    private Integer idMondo;       // solo in creazione
    private Integer idSistema;     // solo in creazione
    private String descrizione;

    // Info Razza: campi puramente descrittivi, valorizzati solo per tipo RAZZA.
    private String razzaTaglia;
    private String razzaVelocita;
    private String razzaCaratteristiche;
    private String razzaLap;          // LAP = Level Adjustment
    private String razzaSpazio;
    private String razzaPortata;

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

    /**
     * Formula gradi quando la classe è il livello 1 del personaggio (label RANK_1).
     */
    private String rank1;

    /**
     * Formula gradi per tutti gli altri livelli (label RANK).
     */
    private String rank;

    /**
     * Numero di livelli della classe (label LIVELLI_CLASSE). Default 20.
     */
    private Integer numLivelli;

    /**
     * Dadi vita della classe (label DV), es. "2d10". Un solo valore per classe:
     * pre-valorizza il DV di ogni livello preso in questa classe.
     */
    private String dv;

    private List<LivelloClasseDTO> livelli;

    private List<AbilitaConcessaDTO> abilitaConcesse;

    /**
     * Sezioni incantatore: ognuna ha 1..N liste (unite), una progressione (preset o CUSTOM)
     * e una formula slot bonus. Se valorizzata, sostituisce il vecchio spellList singolo.
     */
    private List<SezioneSpellDTO> sezioniIncantesimi;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SezioneSpellDTO {
        private List<String> liste;       // id liste incantesimi
        private String progressione;      // MAGO/CHIERICO/.../CUSTOM
        private String bonus;             // formula slot bonus
        private List<String> slot;        // CUSTOM: tabella slot per livello di classe (una riga "4,2,1,…" per livello)

        /**
         * Incantesimi conosciuti: concetto separato dagli slot/giorno (rilevante per incantatori
         * spontanei come Bardo/Stregone). Opzionale/flaggabile: se conosciutiSeparati è false,
         * conosciuti è ignorato. Nessuna formula di bonus: il bonus da caratteristica si applica
         * solo agli slot, mai al numero di incantesimi conosciuti.
         */
        private boolean conosciutiSeparati;
        private List<String> conosciuti;  // stesso formato/dash di "slot" (una riga "4,2,1,-,…" per livello)
    }

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
        private Integer qty;
    }
}
