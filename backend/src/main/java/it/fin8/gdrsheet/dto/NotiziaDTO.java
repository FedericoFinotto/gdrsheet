package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotiziaDTO {
    private Integer id;
    private String nome;
    private String descrizione;
    private String dataInizio;
    private String dataFine;
    private boolean archiviata;
}
