package it.fin8.gdrsheet.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Spostamento di un item dall'inventario di un personaggio a un altro.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GiveItemRequest {
    @NotNull
    private Integer itemId;
    @NotNull
    private Integer fromPersonaggioId;
    @NotNull
    private Integer toPersonaggioId;
}
