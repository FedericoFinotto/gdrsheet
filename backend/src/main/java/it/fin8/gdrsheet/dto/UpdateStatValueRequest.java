package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatValueRequest {
    private Integer idPersonaggio;
    private String idStat;
    private String valore;
    private String formula;
    private String modStatId;
}
