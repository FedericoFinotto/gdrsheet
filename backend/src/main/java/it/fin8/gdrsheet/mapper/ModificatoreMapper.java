package it.fin8.gdrsheet.mapper;

import it.fin8.gdrsheet.config.Constants;
import it.fin8.gdrsheet.dto.AbilitaClasseDTO;
import it.fin8.gdrsheet.dto.ModificatoreDTO;
import it.fin8.gdrsheet.dto.RankDTO;
import it.fin8.gdrsheet.entity.Item;
import it.fin8.gdrsheet.entity.Modificatore;
import it.fin8.gdrsheet.repository.ItemRepository;
import it.fin8.gdrsheet.repository.PersonaggioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ModificatoreMapper {


    @Autowired
    PersonaggioRepository personaggioRepository;

    @Autowired
    ItemRepository itemRepository;

    public ModificatoreDTO toDTO(Modificatore entity) {
        ModificatoreDTO dto = new ModificatoreDTO();
        dto.setId(entity.getId());
        dto.setTipo(entity.getTipo());
        dto.setFormula(entity.getValore());
        try {
            dto.setValore(Integer.parseInt(entity.getValore()));
        } catch (Exception e) {
        }
        dto.setNota(entity.getNota());
        dto.setPermanente(entity.getSempreAttivo());
        dto.setItem(entity.getItem().getNome());
        dto.setStatId(entity.getStat().getId());
        dto.setTipoItem(entity.getItem().getTipo());
        return dto;
    }

    public RankDTO toRankDTO(Modificatore entity, Map<String, List<AbilitaClasseDTO>> mappaAbilitaClasse) {
        Item livello = entity.getItem();
        List<AbilitaClasseDTO> listaAbilitaDiClasse = mappaAbilitaClasse.get(livello.getLabel(Constants.ITEM_LIVELLO_LVL));

        RankDTO dto = new RankDTO();
        dto.setId(entity.getId());
        dto.setTipo(entity.getTipo());
        dto.setValore(Integer.parseInt(entity.getValore()));
        dto.setNota(entity.getNota());


        dto.setSempreAttivo(entity.getSempreAttivo());
        dto.setItem(entity.getItem().getNome());
        dto.setItemId(entity.getItem().getId());
        dto.setStatId(entity.getStat().getId());

        dto.setDiClasse(false);
        listaAbilitaDiClasse.stream().filter(x -> x.getId().equals(entity.getStat().getId())).findFirst().ifPresent(x -> {
            dto.setDiClasse(true);
            dto.setClasse(x.getClasse().get(0).getNome());
        });

        return dto;
    }
}
