package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Voce della ricerca "collega una quest esistente come sotto-quest": solo quest radice (non
 * già figlie di un'altra quest), con l'ambito in chiaro per distinguere omonimie.
 */
@Getter
@Setter
@AllArgsConstructor
public class QuestSceltaDTO {
    private Integer id;
    private String nome;
    /** "Party" | "Mondo" | "Personaggio: <nome>" | null se non determinabile. */
    private String ambito;
}
