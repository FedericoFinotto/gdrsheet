package it.fin8.gdrsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import it.fin8.gdrsheet.dto.GiveItemRequest;
import it.fin8.gdrsheet.dto.PageDTO;
import it.fin8.gdrsheet.dto.PartyDetailDTO;
import it.fin8.gdrsheet.dto.PartyItemDTO;
import it.fin8.gdrsheet.entity.Utente;
import it.fin8.gdrsheet.service.PartyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/party")
public class PartyController {

    private final PartyService partyService;

    public PartyController(PartyService partyService) {
        this.partyService = partyService;
    }

    @Operation(
            summary = "Dettaglio party",
            description = "Membri del party con i loro soldi (somma dei modificatori MR/MA/MO/MP) e somma complessiva. Accessibile solo ai membri del party."
    )
    @GetMapping("/{id}")
    public ResponseEntity<PartyDetailDTO> getParty(
            @Parameter(description = "Id Party", required = true)
            @PathVariable Integer id,
            @AuthenticationPrincipal Utente utente
    ) {
        return ResponseEntity.ok(partyService.getPartyDetail(id, utente));
    }

    @Operation(
            summary = "Item del party",
            description = "Item di inventario dei membri del party, filtrati per nome/tipo e paginati"
    )
    @GetMapping("/{id}/items")
    public ResponseEntity<PageDTO<PartyItemDTO>> getPartyItems(
            @Parameter(description = "Id Party", required = true)
            @PathVariable Integer id,
            @Parameter(description = "Filtro nome (contains, case-insensitive)")
            @RequestParam(required = false) String nome,
            @Parameter(description = "Filtro tipo item")
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal Utente utente
    ) {
        return ResponseEntity.ok(partyService.getPartyItems(id, utente, nome, tipo, page, size));
    }

    @Operation(
            summary = "Sposta un item tra inventari",
            description = "Sposta un item dall'inventario di un personaggio a un altro dello stesso party"
    )
    @PostMapping("/give")
    public ResponseEntity<Void> giveItem(
            @Valid @RequestBody GiveItemRequest request,
            @AuthenticationPrincipal Utente utente
    ) {
        partyService.giveItem(request, utente);
        return ResponseEntity.noContent().build();
    }
}
