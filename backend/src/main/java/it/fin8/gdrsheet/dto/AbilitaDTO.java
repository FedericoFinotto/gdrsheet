package it.fin8.gdrsheet.dto;

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
public class AbilitaDTO {
    private ABILITA abilita;
    private RANK rank;
    private CaratteristicaDTO base;

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
        abilita = new ABILITA(stat.getStat().getId(), stat.getStat().getLabel(), modificatore, modificatori, stat.getAddestramento());
        rank = new RANK(valoreRank, modRank, ranks);
        base = caratteristicaBase;

    }

    public Boolean getShow() {
        return !abilita.getAddestramento() || rank.getModificatore() != 0;
    }
}

