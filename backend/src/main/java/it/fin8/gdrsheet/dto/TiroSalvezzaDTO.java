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
    Integer modificatore;
    List<ModificatoreDTO> modificatori;

}
