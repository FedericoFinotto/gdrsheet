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
public class AttributoDTO {
    String id;
    String label;
    Integer modificatore;
    List<ModificatoreDTO> modificatori;
}
