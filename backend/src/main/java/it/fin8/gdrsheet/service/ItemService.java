package it.fin8.gdrsheet.service;

import it.fin8.gdrsheet.entity.Item;
import it.fin8.gdrsheet.entity.ItemLabel;
import it.fin8.gdrsheet.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ItemService {

    @Autowired
    private ItemRepository itemRepository;

    public Item switchItemState(Integer itemId) {
        Item itm = itemRepository.findItemById(itemId);
        Optional<ItemLabel> label = itm.getLabels().stream().filter(x -> x.getLabel().equals("DISABLED")).findFirst();
        if (label.isPresent()) {
            ItemLabel itemLabel = label.get();
            String valore = itemLabel.getValore();
            if (valore.equals("0")) {
                itemLabel.setValore("1");
            } else {
                itemLabel.setValore("0");
            }
        } else {
            ItemLabel itemLabel = new ItemLabel();
            itemLabel.setItem(itm);
            itemLabel.setLabel("DISABLED");
            itemLabel.setValore("1");
            itm.getLabels().add(itemLabel);
        }
        itemRepository.save(itm);
        return itm;
    }
}

