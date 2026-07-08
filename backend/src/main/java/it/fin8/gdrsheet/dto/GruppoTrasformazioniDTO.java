package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Gruppo di trasformazioni mutuamente esclusive (stesso "gruppo").
 * Per le forme collegate a un FRUTTO il gruppo è impostato esplicitamente a "FORMA".
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GruppoTrasformazioniDTO {
    private String gruppo;
    private List<TrasformazioneDTO> trasformazioni;
}
