package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Riga stat_default per la pagina admin (lettura + scrittura). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatDefaultDTO {
    private Integer id;             // null in creazione
    private Integer mondoId;
    private String statId;
    private String statLabel;       // solo lettura
    private String valoreDefault;
    private String defaultModId;    // stat usata come modificatore (opzionale)
    private String defaultModLabel; // solo lettura
    private Boolean addestramento;
    // Ha una colonna nella Tabella Livelli di una classe per questo mondo, e con che modo di
    // aggregazione multi-classe (SOMMATIVO|SOSTITUTIVO, vedi Constants) — vedi db.changelog-21.0.xml.
    private Boolean livelloClasse;
    private String modoLivelloClasse;
}
