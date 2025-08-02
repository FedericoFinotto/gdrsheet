package it.fin8.grdsheet.dto;

import it.fin8.grdsheet.entity.Item;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ItemLivelloDTO {
    private Item item;
    private String livello;
}
