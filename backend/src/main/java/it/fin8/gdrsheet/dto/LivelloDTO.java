package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LivelloDTO extends ItemDTO {
    Integer livello;
    String classe;
    Integer classeId;
    String maledizione;
    List<Integer> livelliClasse;
    Integer gradi; // valore congelato GRADI_LIVELLO (punti assegnabili a questo livello), null se non impostato
    // Modificatore di Intelligenza che, nella formula RANK/RANK_1 della classe, produrrebbe
    // esattamente "gradi" (reverse-solve): null se manca la classe/formula o nessun match esatto.
    Integer intModEquivalente;
}
