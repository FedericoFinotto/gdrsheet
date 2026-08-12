package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CalcoloResponse {
    private String risultato;
    private String formula;
    // Formula originale con le variabili (@FOR, @1191_CARICHE, ...) già sostituite dai valori del
    // personaggio, ma SENZA valutare l'espressione aritmetica (dadi/operatori restano testo) — per
    // mostrarla all'utente in scheda invece di richiedere una seconda risoluzione lato frontend
    // (vedi CalcoloService.sostituisciVariabili).
    private String formulaRisolta;
    // Formula "leggibile" per il sotto-testo: solo le variabili opache (contatori item, taglia,
    // livelli per classe) sostituite col valore, le caratteristiche (FOR, SAG, ...) lasciate
    // simboliche — vedi CalcoloService.formulaLeggibile.
    private String formulaLeggibile;
}

