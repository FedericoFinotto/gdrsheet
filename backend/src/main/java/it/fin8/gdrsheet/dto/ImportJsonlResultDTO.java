package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Esito di un import bulk da file JSONL (vedi ItemImportService).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImportJsonlResultDTO {

    private int totalRows;
    private int created;
    private int skipped;
    private List<ImportRowErrorDTO> errors;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportRowErrorDTO {
        private int riga;
        private String messaggio;
    }
}
