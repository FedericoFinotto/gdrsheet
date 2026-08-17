package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un utente con un permesso (MASTER, STATS o PAGINE — vedi TipoPermessoMondo) su un mondo
 * (tabella permessi_mondo). Nome della classe invariato per compatibilità, anche se non riguarda
 * più solo MASTER.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MasterMondoDTO {
    private Integer utenteId;
    private String username;
    private String name;
    private String permesso;
}
