package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Esito dell'import bulk di classi da un file JSONL (vedi ClassImportService,
 * pensato per il classes.jsonl prodotto da scripts/dndtools-scraper/scrape_classes.py).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassImportResultDTO {

    private int totalRows;
    private int classiCreate;
    private int classiSaltate;
    private int livelliCreati;
    private int abilitaCreate;
    private List<ImportRowErrorDTO> errors;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportRowErrorDTO {
        private int riga;
        private String nomeClasse;
        private String messaggio;
    }
}
