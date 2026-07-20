package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SegnalazioneDTO {
    private Integer id;
    private Integer ref;
    private String titolo;
    private String descrizione;
    private String stato;
    private String dataCreazione;
    private String dataModifica;
    private boolean mia;
}
