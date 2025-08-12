package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ItemsDTO {
    private List<ItemDTO> abilita;
    private List<ItemDTO> talenti;
    private List<ItemDTO> oggetti;
    private List<ItemDTO> consumabili;
    private List<ItemDTO> armi;
    private List<ItemDTO> munizioni;
    private List<ItemDTO> equipaggiamento;
    private List<ItemDTO> classi;
    private List<ItemDTO> razze;
    private List<AttaccoDTO> attacchi;
    private List<ItemDTO> livelli;
    private List<ItemDTO> maledizioni;
    //    private List<IncantesimoDTO> incantesimi;
    private List<SpellBookDTO> spellbooks;
    private List<ItemDTO> trasformazioni;
    private List<ItemDTO> competenze;
    private List<ItemDTO> lingue;

    public ItemsDTO() {
        abilita = new ArrayList<>();
        talenti = new ArrayList<>();
        oggetti = new ArrayList<>();
        consumabili = new ArrayList<>();
        armi = new ArrayList<>();
        munizioni = new ArrayList<>();
        equipaggiamento = new ArrayList<>();
        classi = new ArrayList<>();
        razze = new ArrayList<>();
        attacchi = new ArrayList<>();
        livelli = new ArrayList<>();
        maledizioni = new ArrayList<>();
        spellbooks = new ArrayList<>();
        trasformazioni = new ArrayList<>();
        competenze = new ArrayList<>();
        lingue = new ArrayList<>();
    }
}
