package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Nodo di un albero di INFO (radice o sotto-info): stessa organizzazione ad albero e stesso
 * ambito (PARTY/MONDO/PERSONAGGIO) delle quest (vedi QuestDTO), ma senza completamento né
 * "in carico" — il contenuto è la lista di note, ciascuna con una propria visibilità.
 */
@Getter
@Setter
@AllArgsConstructor
public class InfoDTO {
    private Integer id;
    private String nome;
    private String descrizione;
    private List<NotaDTO> note;
    private List<InfoDTO> figli;
    /** Solo per gli INFO radice: "PARTY" | "MONDO" | "PERSONAGGIO". Null per i sotto-info. */
    private String ambito;
    /** Solo per gli INFO radice di ambito PERSONAGGIO: nome del personaggio a cui è associato. */
    private String personaggioNome;
}
