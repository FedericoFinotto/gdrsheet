package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SpellBookIncantesimoDTO extends ItemDTO {
    private String cd;
    private Integer livello;
    private Integer idClasse;
    private String classe;
    private String spellList;
    private Integer nPrepared;
    private Integer nUsed;
    private Boolean alwaysPrep;
    private List<String> componenti;
}
