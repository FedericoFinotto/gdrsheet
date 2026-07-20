package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AllegatoDTO {
    private Integer id;
    private String nome;
    private String url;
}
