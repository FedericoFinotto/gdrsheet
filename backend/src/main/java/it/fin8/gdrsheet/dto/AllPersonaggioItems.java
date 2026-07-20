package it.fin8.gdrsheet.dto;

import it.fin8.gdrsheet.entity.Item;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
public class AllPersonaggioItems {
    private List<Item> items;
    private InfoLivelliDTO livelli;
    private Map<Item, List<ItemLivelloDTO>> incantesimi;
    /** ID dei FRUTTO la cui scelta NON include MOD: i loro modificatori non vanno calcolati. */
    private Set<Integer> fruttiSenzaMod;
    /** Id item -> prefisso (label PREFISSO_OGGETTI) dell'item "virtualmente genitore": usato per
     *  gli item concessi da ADD_CLASSE_&lt;n&gt;_ITEMS, che non hanno un vero Collegamento parent
     *  (item.getParent() non aiuta) ma vanno comunque mostrati col chip prefisso se l'item che
     *  porta la label ADD_CLASSE lo imposta. */
    private Map<Integer, String> prefissoVirtualeById;

    public AllPersonaggioItems() {
        items = new ArrayList<>();
        incantesimi = new HashMap<>();
        fruttiSenzaMod = new HashSet<>();
        prefissoVirtualeById = new HashMap<>();
    }
}
