package it.fin8.gdrsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import it.fin8.gdrsheet.dto.PartyDetailDTO;
import it.fin8.gdrsheet.entity.Utente;
import it.fin8.gdrsheet.service.PartyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
