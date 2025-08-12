package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class SpellBookDTO {
    Integer idClasse;
    String nomeClasse;
    String spellList;
    List<SpellBookLivelloDTO> livelli;

    public SpellBookDTO() {
        livelli = new ArrayList<>();
    }
}
