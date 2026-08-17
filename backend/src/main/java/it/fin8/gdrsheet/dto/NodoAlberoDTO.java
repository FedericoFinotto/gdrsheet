package it.fin8.gdrsheet.dto;

import java.util.List;

/**
 * Un nodo dell'albero grafico NODO (vedi TipoItem.NODO): id/nome per disegnare il box, tipoNome
 * (se il nodo ha un "Tipo" collegato, cioè il link singolo a un item non-NODO) come sottotitolo
 * opzionale, e figli = gli id dei nodi "A" (verso cui si può andare da qui), già filtrati lato
 * service a quelli presenti nello stesso albero — il frontend deriva le radici (nessun genitore)
 * e la disposizione a livelli da questi archi, senza bisogno di un'altra chiamata per il "Da".
 */
public record NodoAlberoDTO(Integer id, String nome, String tipoNome, List<Integer> figli) {
}
