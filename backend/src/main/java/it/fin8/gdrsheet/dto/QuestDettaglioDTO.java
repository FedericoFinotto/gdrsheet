package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Parte "pesante" di una quest (testo della descrizione e note in rich text), caricata solo
 * quando l'utente apre effettivamente quel nodo: l'albero restituito da /quest/personaggio e
 * /quest/party porta soltanto la struttura e i conteggi.
 */
@Getter
@Setter
@AllArgsConstructor
public class QuestDettaglioDTO {
    private Integer id;
    private String descrizione;
    /** Già filtrate lato server in base a chi guarda: solo le note effettivamente visibili. */
    private List<NotaDTO> note;
}
