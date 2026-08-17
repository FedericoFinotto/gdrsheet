package it.fin8.gdrsheet.dto;

import it.fin8.gdrsheet.def.TipoPermessoMondo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Richiesta di assegnare un permesso (MASTER, STATS o PAGINE — indipendenti tra loro, vedi
 * TipoPermessoMondo) a un utente su un mondo. Nome della classe invariato per compatibilità con
 * l'endpoint esistente, anche se non riguarda più solo MASTER.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddMasterMondoRequest {
    @NotBlank
    private String username;
    @NotNull
    private TipoPermessoMondo permesso;
}
