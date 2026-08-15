package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class SpellBookLivelloDTO {
    Integer livello;
    Integer slot;
    Integer conosciuti;  // null = la sezione non traccia gli incantesimi conosciuti separatamente
    // true = la sezione traccia gli slot usati per QUESTO livello con un contatore dedicato
    // (SPELL_<n>_SLOT_CONTATORE sulla sezione) invece del semplice numero statico "slot".
    boolean slotConContatore;
    // Slot già usati a questo livello (0 se non tracciato/mai usato) — scoped per personaggio,
    // vedi ItemService.getSlotUsatiPerLivello/setSlotUsatiPerLivello.
    Integer slotUsati;
    List<String> bonus;
    List<SpellBookIncantesimoDTO> incantesimi;

    public SpellBookLivelloDTO() {
        bonus = new ArrayList<>();
        incantesimi = new ArrayList<>();
    }
}
