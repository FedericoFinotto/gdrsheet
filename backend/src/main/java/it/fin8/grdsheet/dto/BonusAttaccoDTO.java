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
public class BonusAttaccoDTO {
    String id;
    String nome;
    Integer modificatore;
    List<Integer> attacchiMultipli;
    List<ModificatoreDTO> modificatori;
}