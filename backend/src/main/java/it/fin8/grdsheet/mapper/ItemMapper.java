package it.fin8.grdsheet.mapper;

import it.fin8.grdsheet.dto.ItemDTO;
import it.fin8.grdsheet.entity.Item;
import org.springframework.stereotype.Component;

@Component
public class ItemMapper {

    public ItemDTO toDTO(Item entity) {
        ItemDTO dto = new ItemDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setTipo(entity.getTipo());
        return dto;
    }
}
