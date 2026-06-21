package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private UtenteDTO utente;
    private boolean mustSetPassword;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UtenteDTO {
        private Integer id;
        private String username;
        private String name;
        private String ruolo;
    }
}
