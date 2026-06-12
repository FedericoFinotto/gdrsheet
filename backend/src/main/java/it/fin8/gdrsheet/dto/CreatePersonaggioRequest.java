package it.fin8.gdrsheet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePersonaggioRequest {
    @NotNull
    private Integer partyId;
    @NotBlank
    private String nome;
    /**
     * Tipo personaggio: PG | NPC | BARCA | BANCA | STELLA.
     * PG = personaggio normale (nessuna label TIPO_PERSONAGGIO).
     */
    @NotBlank
    private String tipo;

    /**
     * Solo per i PG: utente proprietario (dev'essere membro del party).
     * Se assente, il proprietario è chi crea.
     */
    private Integer proprietarioUtenteId;
}
