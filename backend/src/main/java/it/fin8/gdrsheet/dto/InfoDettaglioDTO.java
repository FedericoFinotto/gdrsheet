package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Parte "pesante" di un INFO (descrizione e note in rich text), caricata solo quando l'utente
 * apre effettivamente quel nodo — stesso pattern di QuestDettaglioDTO.
 */
@Getter
@Setter
@AllArgsConstructor
public class InfoDettaglioDTO {
    private Integer id;
    private String descrizione;
    /** Già filtrate lato server in base a chi guarda: solo le note effettivamente visibili. */
    private List<NotaDTO> note;
}
