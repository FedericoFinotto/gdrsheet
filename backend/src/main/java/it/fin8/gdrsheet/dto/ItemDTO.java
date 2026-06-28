package it.fin8.gdrsheet.dto;

import it.fin8.gdrsheet.def.TipoItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ItemDTO {
    private Integer id;
    private String nome;
    private TipoItem tipo;
    private Boolean disabled;
    /**
     * Quantità (label QTA), default 1.
     */
    private Integer quantita;
    /**
     * Barriera (talento con label TIPO=BARRIERA): hp temporanei "blu".
     */
    private Boolean barriera;
    private Integer barrMax;
    private Integer barrCons;
    /**
     * True se l'item è collegato direttamente via FromCompendio (può essere scollegato dall'utente).
     */
    private Boolean scollegabile;
    /**
     * Utilizzi massimi (label UTILIZZI sull'item, globale). Null = nessun limite definito.
     */
    private Integer utilizziTotale;
    /**
     * Utilizzi consumati dal personaggio corrente (label UTILIZZI_USATI per-personaggio).
     */
    private Integer utilizziUsati;
}
