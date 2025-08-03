package it.fin8.grdsheet.mapper;

import it.fin8.grdsheet.def.TipoItem;
import it.fin8.grdsheet.dto.ModificatoreDTO;
import it.fin8.grdsheet.dto.RankDTO;
import it.fin8.grdsheet.entity.Collegamento;
import it.fin8.grdsheet.entity.Item;
import it.fin8.grdsheet.entity.ItemLabel;
import it.fin8.grdsheet.entity.Modificatore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ModificatoreMapper {

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
        dto.setSempreAttivo(entity.getSempreAttivo());
        dto.setItem(entity.getItem().getNome());
        dto.setStatId(entity.getStat().getId());
        return dto;
    }

    public RankDTO toRankDTO(Modificatore entity, List<ItemLabel> abClasse) {
        String nomeClasse = null;
        boolean diClasse = false;
        Item classe = entity.getItem().getChild().stream().map(Collegamento::getItemTarget).filter(itemTarget -> itemTarget.getTipo().equals(TipoItem.CLASSE)).findFirst().orElse(null);
        Map<String, List<String>> mappaDiClasse = abClasse.stream()
                .filter(x ->
                        x.getItem().getTipo() != TipoItem.CLASSE
                                || x.getItem().equals(classe)
                )
                .collect(Collectors.toMap(
                        x -> x.getItem().getNome(),
                        x -> List.of(x.getValore().split(","))
                ));

        for (Map.Entry<String, List<String>> entry : mappaDiClasse.entrySet()) {
            List<String> valori = entry.getValue();
            String idStat = String.valueOf(entity.getStat().getId());
            if (valori.contains(idStat)) {
                diClasse = true;
                nomeClasse = entry.getKey();
                break;
            }
        }
        RankDTO dto = new RankDTO();
        dto.setId(entity.getId());
        dto.setTipo(entity.getTipo());
        dto.setValore(Integer.parseInt(entity.getValore()));
        dto.setNota(entity.getNota());
        dto.setDiClasse(diClasse);
        dto.setClasse(nomeClasse);
        dto.setSempreAttivo(entity.getSempreAttivo());
        dto.setItem(entity.getItem().getNome());
        dto.setStatId(entity.getStat().getId());
        return dto;
    }
}
