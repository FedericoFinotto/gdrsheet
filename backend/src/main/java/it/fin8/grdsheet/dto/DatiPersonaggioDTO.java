package it.fin8.grdsheet.dto;

import it.fin8.grdsheet.entity.Personaggio;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
@Setter
@NoArgsConstructor
public class DatiPersonaggioDTO {

    Integer id;
    String nome;

    List<CaratteristicaDTO> caratteristiche;
    List<TiroSalvezzaDTO> tiriSalvezza;
    List<AbilitaDTO> abilita;
    List<ClasseArmaturaDTO> classeArmatura;
    List<BonusAttaccoDTO> bonusAttacco;

    public DatiPersonaggioDTO(Personaggio personaggio) {
        id = personaggio.getId();
        nome = personaggio.getNome();
        caratteristiche = new ArrayList<>();
        abilita = new ArrayList<>();
        tiriSalvezza = new ArrayList<>();
        classeArmatura = new ArrayList<>();
        bonusAttacco = new ArrayList<>();
    }

}
