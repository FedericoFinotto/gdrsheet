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
public class TiroSalvezzaDTO {

    String id;
    String label;
    String idBase;
    Integer modBase;
    Integer modificatore;
    List<ModificatoreDTO> modificatori;

}
