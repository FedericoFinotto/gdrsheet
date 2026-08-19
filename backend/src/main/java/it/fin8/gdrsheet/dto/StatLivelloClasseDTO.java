package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Una stat con colonna nella Tabella Livelli di una classe, per un dato mondo (vedi
 * StatController#getLivelloClasse e db.changelog-21.0.xml). Solo lettura, aperto a
 * qualunque utente autenticato.
 */
@Getter
@Setter
@AllArgsConstructor
public class StatLivelloClasseDTO {
    private String statId;
    private String statLabel;
    private String modo; // SOMMATIVO o SOSTITUTIVO, vedi Constants
}
