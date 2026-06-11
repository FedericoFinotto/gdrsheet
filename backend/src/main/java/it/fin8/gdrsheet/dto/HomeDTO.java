package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Dati mostrati all'apertura dell'app: i party dell'utente (con ruolo
 * MASTER/GIOCATORE) e i personaggi a cui ha accesso (PROPRIETARIO/VISUALIZZATORE).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HomeDTO {
    private LoginResponse.UtenteDTO utente;
    private List<PartyHomeDTO> parties;
    private List<PersonaggioHomeDTO> personaggi;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartyHomeDTO {
        private Integer id;
        private String nome;
        private String ruolo; // MASTER | GIOCATORE
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonaggioHomeDTO {
        private Integer id;
        private String nome;
        private String permesso; // PROPRIETARIO | VISUALIZZATORE
        private Integer partyId;
        private String partyNome;
    }
}
