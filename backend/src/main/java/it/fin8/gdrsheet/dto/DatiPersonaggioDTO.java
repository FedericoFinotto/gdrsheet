package it.fin8.gdrsheet.dto;

import it.fin8.gdrsheet.entity.Personaggio;
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
    List<ContatoreDTO> contatori;
    List<AttributoDTO> attributi;
    List<ContatoreItemDTO> contatoriItem;
    DadiVitaDTO dadiVita;

    public DatiPersonaggioDTO(Personaggio personaggio) {
        id = personaggio.getId();
        nome = personaggio.getNome();
        caratteristiche = new ArrayList<>();
        abilita = new ArrayList<>();
        tiriSalvezza = new ArrayList<>();
        classeArmatura = new ArrayList<>();
        bonusAttacco = new ArrayList<>();
        contatori = new ArrayList<>();
        attributi = new ArrayList<>();
        contatoriItem = new ArrayList<>();
    }

    public CaratteristicaDTO getCaratteristica(String id) {
        return caratteristiche.stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
    }

}
