package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Dettaglio party: membri (inclusi "personaggi-barca" e simili) con i loro
 * soldi e la somma complessiva del party.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PartyDetailDTO {
    private Integer id;
    private String nome;
    private String ruolo; // ruolo dell'utente corrente nel party: MASTER | GIOCATORE
    private List<PersonaggioSoldiDTO> personaggi;
    private SoldiDTO somma;

    /**
     * Monete: MR rame, MA argento, MO oro, MP platino.
     * 100 MR = 10 MA = 1 MO; 1 MP = 10 MO.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SoldiDTO {
        private long mr;
        private long ma;
        private long mo;
        private long mp;

        public void add(SoldiDTO other) {
            this.mr += other.mr;
            this.ma += other.ma;
            this.mo += other.mo;
            this.mp += other.mp;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonaggioSoldiDTO {
        private Integer id;
        private String nome;
        private SoldiDTO soldi;
        /**
         * Valore della personaggio_label TIPO_PERSONAGGIO (es. NAVE, STELLA); null = personaggio normale.
         */
        private String tipoPersonaggio;
        /**
         * True se l'utente corrente è proprietario del personaggio.
         */
        private boolean proprietario;
    }
}
