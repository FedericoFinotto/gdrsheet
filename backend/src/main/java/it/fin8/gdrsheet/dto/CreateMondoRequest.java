package it.fin8.gdrsheet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMondoRequest {
    @NotBlank
    private String descrizione;
    /** Obbligatorio: sistema_id è NOT NULL a livello di schema. */
    @NotNull
    private Integer sistemaId;
}
