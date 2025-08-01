package it.fin8.grdsheet.mapper;

import it.fin8.grdsheet.def.TipoItem;
import it.fin8.grdsheet.dto.ModificatoreDTO;
import it.fin8.grdsheet.dto.RankDTO;
import it.fin8.grdsheet.entity.Collegamento;
import it.fin8.grdsheet.entity.Item;
import it.fin8.grdsheet.entity.ItemLabel;
import it.fin8.grdsheet.entity.Modificatore;
import org.springframework.stereotype.Component;

@Component
public class ModificatoreMapper {

    public ModificatoreDTO toDTO(Modificatore entity) {
        ModificatoreDTO dto = new ModificatoreDTO();
        dto.setId(entity.getId());
        dto.setTipo(entity.getTipo());
        dto.setValore(Integer.parseInt(entity.getValore()));
        dto.setNota(entity.getNota());
        dto.setSempreAttivo(entity.getSempreAttivo());
        return dto;
    }

    public RankDTO toRankDTO(Modificatore entity) {
        String nomeClasse = null;
        boolean diClasse = false;
        Item classe = entity.getItem().getChild().stream().map(Collegamento::getItemTarget).filter(itemTarget -> itemTarget.getTipo().equals(TipoItem.CLASSE)).findFirst().orElse(null);
        if (classe != null) {
            ItemLabel ABCLASSE = classe.getLabels().stream().filter(x -> x.getLabel().equals("ABCLASSE")).findFirst().orElse(null);
            diClasse = ABCLASSE != null && ABCLASSE.getValore().contains(entity.getStat().getId());
            nomeClasse = classe.getNome();
        }
        RankDTO dto = new RankDTO();
        dto.setId(entity.getId());
        dto.setTipo(entity.getTipo());
        dto.setValore(Integer.parseInt(entity.getValore()));
        dto.setNota(entity.getNota());
        dto.setDiClasse(diClasse);
        dto.setClasse(nomeClasse);
        dto.setSempreAttivo(entity.getSempreAttivo());
        return dto;
    }
}
