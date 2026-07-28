package it.fin8.gdrsheet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSistemaRequest {
    @NotBlank
    private String descrizione;
}
