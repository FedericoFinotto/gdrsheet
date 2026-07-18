package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Nodo di un albero di quest (radice o sotto-quest). Il completamento si calcola
 * ricorsivamente: una quest senza sotto-quest è "foglia" (0/1 o 1/1 in base al proprio
 * stato); una quest con sotto-quest somma le foglie completate/totali dell'intero
 * sottoalbero, propagando così il completamento verso ogni antenato.
 */
@Getter
@Setter
@AllArgsConstructor
public class QuestDTO {
    private Integer id;
    private String nome;
    private String descrizione;
    /** Stato proprio di completamento: significativo solo per le quest foglia (senza figli). */
    private Boolean completata;
    /** Foglie completate nell'intero sottoalbero (se stessa inclusa, se foglia). */
    private Integer completati;
    /** Foglie totali nell'intero sottoalbero. */
    private Integer totali;
    private List<NotaDTO> note;
    private List<QuestDTO> figli;
    /** Solo per le quest radice: "PARTY" | "MONDO" | "PERSONAGGIO". Null per le sotto-quest. */
    private String ambito;
    /** Solo per le quest radice di ambito PERSONAGGIO: nome del personaggio a cui è associata. */
    private String personaggioNome;
}
