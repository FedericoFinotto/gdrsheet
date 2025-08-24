package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AttaccoDTO extends ItemDTO {
    private String attacco;
    private String colpo;
    private String nomeItem;
    private String tiroSalvezza;
    private String tipoDanno;
    private String range;
}
