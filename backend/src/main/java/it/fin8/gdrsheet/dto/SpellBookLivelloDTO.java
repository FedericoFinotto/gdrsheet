package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class SpellBookLivelloDTO {
    Integer livello;
    Integer slot;
    Integer conosciuti;  // null = la sezione non traccia gli incantesimi conosciuti separatamente
    List<String> bonus;
    List<SpellBookIncantesimoDTO> incantesimi;

    public SpellBookLivelloDTO() {
        bonus = new ArrayList<>();
        incantesimi = new ArrayList<>();
    }
}
