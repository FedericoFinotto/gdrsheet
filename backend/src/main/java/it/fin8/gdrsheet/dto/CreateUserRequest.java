package it.fin8.gdrsheet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {
    @NotBlank
    private String username;
    // opzionale: se vuoto viene usato lo username
    private String name;
    // GIOCATORE | MASTER | ADMIN — default GIOCATORE
    private String ruolo;
}
