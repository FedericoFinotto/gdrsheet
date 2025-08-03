package it.fin8.grdsheet.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CalcoloRequest {
    @NotBlank
    private String formula;

    @Valid
    private DatiPersonaggioDTO datiPersonaggio;
}
