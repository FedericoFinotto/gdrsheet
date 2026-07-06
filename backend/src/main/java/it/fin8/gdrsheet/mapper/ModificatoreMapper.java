package it.fin8.gdrsheet.mapper;

import it.fin8.gdrsheet.config.Constants;
import it.fin8.gdrsheet.def.TipoItem;
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
        return toDTO(entity, null);
    }

    /**
     * Overload che accetta una cache (id classe → Item) precaricata in batch dal chiamante,
     * per evitare una query {@code findById} per OGNI modificatore di tipo LIVELLO (i livelli
     * possono avere molti modificatori: caratteristiche base, ranghi, bonus vari). Se la cache
     * è null o non contiene l'id richiesto, ripiega sulla query singola: comportamento identico
     * all'originale, solo più lento in quel caso.
     */
    public ModificatoreDTO toDTO(Modificatore entity, Map<Integer, Item> classiCache) {
        ModificatoreDTO dto = new ModificatoreDTO();
        dto.setId(entity.getId());
        dto.setTipo(entity.getTipo());
        dto.setFormula(entity.getValore());
        try {
            dto.setValore(Integer.parseInt(entity.getValore()));
        } catch (Exception e) {
        }
        dto.setNota(entity.getNota());
        dto.setSempreAttivo(entity.getSempreAttivo());
        dto.setItem(entity.getItem().getNome());
        dto.setItemId(entity.getItem().getId());
        if (TipoItem.LIVELLO.equals(entity.getItem().getTipo())) {
            try {
                Integer classeId = Integer.parseInt(entity.getItem().getLabel(Constants.ITEM_LABEL_CLASSE));
                Item classe = classiCache != null ? classiCache.get(classeId) : null;
                if (classe == null) classe = itemRepository.findById(classeId).orElse(null);
                String livello = entity.getItem().getLabel(Constants.ITEM_LIVELLO_LVL_CLASSE);
                assert classe != null;
                dto.setItem(classe.getNome() + " " + livello);
            } catch (Exception ignored) {
            }
        }

        dto.setStatId(entity.getStat().getId());
        dto.setTipoItem(entity.getItem().getTipo());
        return dto;
    }

    public RankDTO toRankDTO(Modificatore entity, Map<String, List<AbilitaClasseDTO>> mappaAbilitaClasse, Integer livelloPersonaggio) {
        Item livello = entity.getItem();
        List<AbilitaClasseDTO> listaAbilitaDiClasse = mappaAbilitaClasse.get(livello.getLabel(Constants.ITEM_LIVELLO_LVL));

        RankDTO dto = new RankDTO();
        dto.setId(entity.getId());
        dto.setTipo(entity.getTipo());
        dto.setNota(entity.getNota());

        dto.setSempreAttivo(entity.getSempreAttivo());
        dto.setItem(entity.getItem().getNome());
        dto.setItemId(entity.getItem().getId());
        dto.setStatId(entity.getStat().getId());

        dto.setDiClasse(false);
        if (listaAbilitaDiClasse != null) {
            listaAbilitaDiClasse.stream().filter(x -> x.getId().equals(entity.getStat().getId())).findFirst().ifPresent(x -> {
                dto.setDiClasse(x.getAll() || x.getDiClasse());
                dto.setClasse(x.getClasse().get(0).getNome());
            });
        }

        if (Constants.RANK_MAX.equals(entity.getValore())) {
            Integer maxRank = livelloPersonaggio + 3;
            dto.setMaxato(maxRank);
            dto.setValore(maxRank);
            dto.setDiClasse(true);
        } else {
            dto.setValore(Integer.parseInt(entity.getValore()));
        }

        return dto;
    }
}
