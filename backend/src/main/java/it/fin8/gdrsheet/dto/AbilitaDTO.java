package it.fin8.gdrsheet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.fin8.gdrsheet.entity.StatValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties("stat")
public class AbilitaDTO {
    private ABILITA abilita;
    private RANK rank;
    private CaratteristicaDTO base;
    private StatValue stat;
    private Boolean negata;
    private Boolean sbloccata;
    /** Campi editabili del record stat_value associato */
    private String statValore;
    private String statFormula;
    private String statModId;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ABILITA {
        private String id;
        private String nome;
        private Integer modificatore;
        private List<ModificatoreDTO> modificatori;
        private Boolean addestramento;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RANK {
        private Integer valore;
        private Integer modificatore;
        private List<RankDTO> ranks;
    }

    public AbilitaDTO(StatValue stat, Integer modificatore, List<ModificatoreDTO> modificatori, Integer valoreRank, Integer modRank, List<RankDTO> ranks, CaratteristicaDTO caratteristicaBase) {
        this.stat = stat;
        abilita = new ABILITA(stat.getStat().getId(), stat.getStat().getLabel(), modificatore, modificatori, stat.getAddestramento());
        rank = new RANK(valoreRank, modRank, ranks);
        base = caratteristicaBase;
        statValore = stat.getValore();
        statFormula = stat.getFormula();
        statModId = stat.getMod() != null ? stat.getMod().getId() : null;
    }

    public Boolean getShow() {
        if (Boolean.TRUE.equals(negata) || Boolean.TRUE.equals(sbloccata)) return true;
        return !abilita.getAddestramento() || rank.getModificatore() != 0;
    }
}

