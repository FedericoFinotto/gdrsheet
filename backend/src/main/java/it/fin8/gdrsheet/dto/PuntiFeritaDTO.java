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
public class PuntiFeritaDTO {
    String id;
    String nome;
    Integer pf;
    Integer pf_max;
    Integer pf_temp;
    List<ModificatoreDTO> modificatori;
}
