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
    private List<LivelloDTO> livelli;
    private List<ItemDTO> maledizioni;
    private List<SpellBookDTO> spellbooks;
    /** Trasformazioni INDIPENDENTI (non figlie di alcun FRUTTO), già raggruppate per "gruppo". */
    private List<GruppoTrasformazioniDTO> trasformazioni;
    private List<ItemDTO> competenze;
    private List<ItemDTO> lingue;
    private List<ItemDTO> idoli;
    /** Frutti con le loro forme/trasformazioni figlie già raggruppate (vedi FruttoDTO). */
    private List<FruttoDTO> frutti;
    private List<ItemDTO> forme;
    private List<ItemDTO> privilegi;
    private List<ItemDTO> contenitori;
    private List<ItemDTO> altro;
    private List<ItemDTO> notizie;
    private List<ItemDTO> patti;

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
        idoli = new ArrayList<>();
        frutti = new ArrayList<>();
        forme = new ArrayList<>();
        privilegi = new ArrayList<>();
        contenitori = new ArrayList<>();
        altro = new ArrayList<>();
        notizie = new ArrayList<>();
        patti = new ArrayList<>();
    }
}
