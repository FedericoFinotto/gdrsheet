package it.fin8.gdrsheet.dto;

import lombok.Getter;
import lombok.Setter;

/** Creazione/aggiornamento di una stat. */
@Getter
@Setter
public class StatRequest {
    private String id;     // max 10
    private String tipo;   // TipoStat
    private String label;
    private Boolean rankable; // null = non modificare (update) / true (creazione)
}
