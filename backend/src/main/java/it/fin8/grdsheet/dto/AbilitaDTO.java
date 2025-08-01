package it.fin8.grdsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@AllArgsConstructor
@Setter
@NoArgsConstructor
public class AbilitaDTO {

    String id;
    String label;
    String idBase;
    Integer modBase;
    Integer modificatore;
    Integer valoreRank;
    Integer modRank;
    List<ModificatoreDTO> modificatori;
    List<RankDTO> ranks;

}
