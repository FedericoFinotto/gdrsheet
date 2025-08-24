package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@AllArgsConstructor
@Setter
@NoArgsConstructor
public class AbilitaClasseDTO {

    List<IdNomeDTO> classe;
    String id;
    Boolean all;
    Boolean diClasse;

}
