package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePreparedRequest {
    private Integer idPersonaggio;
    private Integer idClasse;
    private String spellList;
    private Integer livello;
    private Map<Integer, Integer> prepared;
}