package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ContatoreItemDTO {
    String id;
    Integer valore;

    public CaratteristicaDTO toCaratteristicaDTO() {
        return new CaratteristicaDTO(id, id, valore, valore, null, null);
    }
}
