package it.fin8.gdrsheet.mapper;

import it.fin8.gdrsheet.dto.BonusAttaccoDTO;
import it.fin8.gdrsheet.dto.CaratteristicaDTO;
import org.springframework.stereotype.Component;

@Component
public class StatMapper {

    public CaratteristicaDTO toCaratteristicaDTO(BonusAttaccoDTO source) {
        CaratteristicaDTO result = new CaratteristicaDTO();
        result.setId(source.getId());
        result.setModificatore(source.getModificatore());
        result.setLabel(source.getNome());
        return result;
    }
}
