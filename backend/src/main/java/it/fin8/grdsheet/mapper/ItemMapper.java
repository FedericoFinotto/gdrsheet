package it.fin8.grdsheet.mapper;

import it.fin8.grdsheet.dto.IncantesimoDTO;
import it.fin8.grdsheet.dto.ItemDTO;
import it.fin8.grdsheet.entity.Item;
import it.fin8.grdsheet.entity.ItemLabel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ItemMapper {

    public ItemDTO toDTO(Item entity) {
        ItemDTO dto = new ItemDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setTipo(entity.getTipo());
        return dto;
    }

    public IncantesimoDTO toIncantesimoDTO(Item entity) {
        List<ItemLabel> itemLabels = entity.getLabels();
        ItemLabel livello = itemLabels.stream().filter(x -> x.getLabel().equals("CLIVELLO")).findFirst().orElse(null);
        ItemLabel classe = itemLabels.stream().filter(x -> x.getLabel().equals("CCLASSE")).findFirst().orElse(null);
        IncantesimoDTO dto = new IncantesimoDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setTipo(entity.getTipo());
        if (livello != null) {
            dto.setLivello(Integer.parseInt(livello.getValore()));
        }
        return dto;
    }
}
