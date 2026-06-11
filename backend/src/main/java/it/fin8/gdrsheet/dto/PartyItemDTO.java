package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Item dell'inventario di un membro del party (vista lista item party).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PartyItemDTO {
    private Integer id;
    private String nome;
    private String tipo;
    private double peso; // kg complessivi (peso unitario x quantità), 0 se assente
    private int quantita;
    private Integer personaggioId;
    private String personaggioNome;
    private boolean disabled;
}
