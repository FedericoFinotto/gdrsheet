package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un utente con permesso MASTER su un mondo (tabella permessi_mondo). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MasterMondoDTO {
    private Integer utenteId;
    private String username;
    private String name;
}
