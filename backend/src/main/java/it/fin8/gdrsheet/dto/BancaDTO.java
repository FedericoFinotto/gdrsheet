package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Personaggio banca (TIPO_PERSONAGGIO=BANCA) con i suoi conti correnti.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BancaDTO {
    private Integer personaggioId;
    private String nome;
    private List<ContoDTO> conti;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContoDTO {
        private Integer itemId;
        /**
         * Label CC: G&lt;idPersonaggio&gt; oppure P&lt;idParty&gt;.
         */
        private String cc;
        private String tipo; // GIOCATORE | PARTY
        private Integer intestatarioId;
        private String intestatarioNome;
        private PartyDetailDTO.SoldiDTO soldi;
    }
}
