package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Immagine di un item. L'identificatore del file presso l'host non viene esposto: serve solo
 * al server per cancellarlo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemImmagineDTO {
    private Integer id;
    private String url;
    private String titolo;
    private Integer ordine;
}
