package it.fin8.gdrsheet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCounterRequest {
    @NotBlank
    private Integer idPersonaggio;

    @NotBlank
    private String id;

    @NotBlank
    private String valore;
}
