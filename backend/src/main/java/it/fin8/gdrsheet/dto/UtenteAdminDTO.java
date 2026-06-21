package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UtenteAdminDTO {
    private Integer id;
    private String username;
    private String name;
    private String ruolo;
    private boolean mustSetPassword;
}
