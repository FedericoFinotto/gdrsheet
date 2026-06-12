package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Vista "scheda" di una banca: conti correnti raggruppati per party.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BancaDetailDTO {
    private Integer personaggioId;
    private String nome;
    private PartyDetailDTO.SoldiDTO totale;
    private List<GruppoPartyDTO> gruppi;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GruppoPartyDTO {
        private Integer partyId;
        private String partyNome;
        private PartyDetailDTO.SoldiDTO totale;
        private List<BancaDTO.ContoDTO> conti;
    }
}
