package it.fin8.gdrsheet.dto;

import lombok.Getter;
import lombok.Setter;

/** Entrambi i campi opzionali: null = non modificare (stesso pattern di UpdateItemRequest). */
@Getter
@Setter
public class UpdateMondoRequest {
    private String descrizione;
    private Integer sistemaId;
}
