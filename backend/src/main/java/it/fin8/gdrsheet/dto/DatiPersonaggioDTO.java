package it.fin8.gdrsheet.dto;

import it.fin8.gdrsheet.entity.Personaggio;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    // Info anagrafiche (personaggio_label): LUOGO_NASCITA, DATA_NASCITA, RAZZA, ...
    Map<String, String> info;
    // Peso totale trasportato (kg): peso personaggio + oggetti + monete
    Double pesoTotale;
    // Taglia attuale (numerica): taglia base + somma ADD_TAGLIA
    Integer tagliaAttuale;

    public DatiPersonaggioDTO(Personaggio personaggio) {
        id = personaggio.getId();
        nome = personaggio.getNome();
        caratteristiche = new ArrayList<>();
        info = new LinkedHashMap<>();
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
