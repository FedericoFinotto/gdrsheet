package it.fin8.gdrsheet.mapper;

import it.fin8.gdrsheet.dto.AttaccoDTO;
import it.fin8.gdrsheet.dto.IncantesimoDTO;
import it.fin8.gdrsheet.dto.ItemDTO;
import it.fin8.gdrsheet.entity.Collegamento;
import it.fin8.gdrsheet.entity.Item;
import it.fin8.gdrsheet.entity.ItemLabel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ItemMapper {

    public ItemDTO toDTO(Item entity) {
        ItemDTO dto = new ItemDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setTipo(entity.getTipo());
        dto.setDisabled(isDisabled(entity));
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

    public AttaccoDTO toAttaccoDTO(Item entity) {
        List<ItemLabel> itemLabels = entity.getLabels();
        ItemLabel attacco = itemLabels.stream().filter(x -> x.getLabel().equals("TPC")).findFirst().orElse(null);
        ItemLabel danno = itemLabels.stream().filter(x -> x.getLabel().equals("TPD")).findFirst().orElse(null);
        List<Collegamento> parents = entity.getParent();
        String nomeItemParent = null;
        if (parents != null && !parents.isEmpty()) {
            Item parent = parents.get(0).getItemSource();
            nomeItemParent = parent.getNome();
        }
        AttaccoDTO dto = new AttaccoDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setTipo(entity.getTipo());
        dto.setNomeItem(nomeItemParent);
        if (attacco != null) {
            dto.setAttacco(attacco.getValore());
        }
        if (danno != null) {
            dto.setColpo(danno.getValore());
        }
        return dto;
    }

    public Boolean isDisabled(Item entity) {
        ItemLabel disabledLabel = entity.getLabels().stream().filter(x -> x.getLabel().equals("DISABLED")).findFirst().orElse(null);
        return disabledLabel != null && disabledLabel.getValore().equals("1");
    }
}
