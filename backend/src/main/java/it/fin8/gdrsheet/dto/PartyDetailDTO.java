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
    /** Gruppi del party (id + nome), per costruire gli accordion. */
    private List<GruppoInfoDTO> gruppi;
    private SoldiDTO somma;
    /**
     * Somma dei pesi (kg) di tutti i membri.
     */
    private double pesoTotale;
    /**
     * Peso (kg) della sola parte monete, già incluso nel totale.
     */
    private double pesoMonete;

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
        /**
         * Peso trasportato in kg: somma delle label PESO degli item del
         * personaggio più la sua eventuale personaggio_label PESO.
         */
        private double peso;
        /** Id del gruppo di appartenenza (personaggio_label GRUPPO); null = nessun gruppo. */
        private Integer gruppoId;
        /** True se è il capogruppo del suo gruppo. */
        private boolean capogruppo;
        /** Livello atteso indicato dalla personaggio_label LIVELLO (solo indicativo); null se assente. */
        private Integer livello;
        /** Numero di livelli effettivi associati (item LIVELLO, escluso l'eventuale livello 0). */
        private int numLivelli;
        /** Gradi Divini indicati dalla personaggio_label GRADI_DIVINI (solo indicativo); null se assente. */
        private Integer gradiDivini;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GruppoInfoDTO {
        private Integer id;
        private String nome;
    }
}
