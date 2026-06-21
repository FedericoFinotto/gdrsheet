package it.fin8.gdrsheet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {
    // assente/ignorato se l'utente non ha ancora una password
    private String oldPassword;
    @NotBlank
    private String newPassword;
}
