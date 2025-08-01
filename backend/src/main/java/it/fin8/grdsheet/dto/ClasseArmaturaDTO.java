package it.fin8.grdsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ClasseArmaturaDTO {
    String id;
    String nome;
    Integer modificatore;
    List<ModificatoreDTO> modificatori;
}
