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
public class UpdateHPRequest {
    @NotBlank
    private Integer idPersonaggio;

    @NotBlank
    private String pf;

    @NotBlank
    private String pfTemp;
}
