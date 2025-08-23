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
public class DadiVitaDTO {
    String id;
    String label;
    Integer totale;
    String totaleStringa;
    List<ModificatoreDTO> modificatori;
}
