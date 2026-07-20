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
    String fonteTipo;  // TipoItem della fonte (CLASSE, OGGETTO, ...): distingue le sezioni in scheda
    String spellList;
    List<SpellBookLivelloDTO> livelli;
    List<SpellBookIncantesimoDTO> spurii;  // incantesimi non da lista/catalogo, con utilizzi propri o di gruppo

    public SpellBookDTO() {
        livelli = new ArrayList<>();
        spurii = new ArrayList<>();
    }
}
