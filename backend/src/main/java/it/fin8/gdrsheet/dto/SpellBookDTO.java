package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class SpellBookDTO {
    Integer idClasse;
    String nomeClasse;
    String fonteTipo;  // TipoItem della fonte (CLASSE, OGGETTO, ...): distingue le sezioni in scheda
    String spellList;
    Integer casterLevel;   // classi: livello effettivo (+ prestigio); oggetti: SPELL_<n>_CASTER_LEVEL fisso
    String caratteristica; // stat id usata per la CD (SPELL_<n>_CARATTERISTICA / SP_CARATTERISTICA)
    Integer cd;            // 10 + casterLevel + modificatore caratteristica; null se non calcolabile
    // Indice "n" della sezione (SPELL_<n>...) sulla fonte (idClasse): serve al frontend per
    // aggiornare il contatore slot usati di un livello (vedi SpellBookLivelloDTO.slotConContatore).
    // Null sulle sezioni legacy (SPELL singola, senza sezioni indicizzate).
    Integer sezioneIndice;
    List<SpellBookLivelloDTO> livelli;
    List<SpellBookIncantesimoDTO> spurii;  // incantesimi non da lista/catalogo, con utilizzi propri o di gruppo

    public SpellBookDTO() {
        livelli = new ArrayList<>();
        spurii = new ArrayList<>();
    }
}
