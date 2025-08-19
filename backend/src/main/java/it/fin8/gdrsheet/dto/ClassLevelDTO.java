package it.fin8.gdrsheet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClassLevelDTO {
    /**
     * Codice SP_* (es. "SP_WIZARD", "SP_DRUID", "SP_FIRE"...)
     */
    private String classe;

    /**
     * Livello incantesimo (0..9) per quella classe/dominio
     */
    private Integer livello;
}