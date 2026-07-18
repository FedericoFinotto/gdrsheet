package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Nota testuale (rich text) associata a un item, con una propria visibilità — stessa
 * convenzione della VISIBILITA a livello di item: ''=tutti, OWNER=solo il proprietario
 * del personaggio (+master/admin), MASTER=solo master/admin. Solo le note effettivamente
 * visibili al richiedente vengono incluse nella risposta (il filtro è già applicato lato
 * server); il campo visibilita viene comunque restituito per mostrare all'utente che la
 * vede a chi altro è (o non è) visibile.
 */
@Getter
@Setter
@AllArgsConstructor
public class NotaDTO {
    private String testo;
    private String visibilita;
}
