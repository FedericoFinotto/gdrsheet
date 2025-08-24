package it.fin8.gdrsheet.dto;

import it.fin8.gdrsheet.entity.Item;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Comparator;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InfoClasseDTO {
    Item classe;
    Set<Integer> livelli;

    public Integer getMax() {
        if (livelli == null || livelli.isEmpty()) return 0;
        return livelli
                .stream()
                .max(Comparator.naturalOrder())
                .orElse(null);
    }
}
