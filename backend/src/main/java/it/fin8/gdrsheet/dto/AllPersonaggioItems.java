package it.fin8.gdrsheet.dto;

import it.fin8.gdrsheet.entity.Item;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class AllPersonaggioItems {
    private List<Item> items;
    private InfoLivelliDTO livelli;
    private Map<Item, List<ItemLivelloDTO>> incantesimi;

    public AllPersonaggioItems() {
        items = new ArrayList<>();
        incantesimi = new HashMap<>();
    }
}
