package it.fin8.gdrsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import it.fin8.gdrsheet.dto.DatiPersonaggioDTO;
import it.fin8.gdrsheet.dto.ItemsDTO;
import it.fin8.gdrsheet.dto.UpdateCounterRequest;
import it.fin8.gdrsheet.dto.UpdateHPRequest;
import it.fin8.gdrsheet.entity.Personaggio;
import it.fin8.gdrsheet.repository.PersonaggioRepository;
import it.fin8.gdrsheet.service.PersonaggioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/personaggi")
public class PersonaggioController {

    private final PersonaggioRepository repo;

    private final PersonaggioService personaggioService;

    public PersonaggioController(PersonaggioRepository repo, PersonaggioService personaggioService) {
        this.repo = repo;
        this.personaggioService = personaggioService;
    }

    @Operation(
            summary = "Recupera un personaggio per ID",
            description = "Restituisce i dati base di un personaggio (nome, razza, classe, livello e riferimenti a utente, mondo, sistema)."
    )
    @GetMapping("/{id}")
    public ResponseEntity<Personaggio> getById(
            @Parameter(description = "ID del personaggio", required = true)
            @PathVariable Integer id
    ) {
        Personaggio p = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Personaggio non trovato"));

        return ResponseEntity.ok(p);
    }

    @Operation(
            summary = "Recupera un personaggio per ID",
            description = "Items legati a un personaggio"
    )
    @GetMapping("/items/{id}")
    public ResponseEntity<ItemsDTO> getAllPersonaggioItemsDTOByIdPersonaggio(
            @Parameter(description = "ID del personaggio", required = true)
            @PathVariable Integer id
    ) {
        ItemsDTO result = personaggioService.getAllPersonaggioItemsDTOByIdPersonaggio(id);

        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Modificatori Personaggio",
            description = "Restituisce i dati base di un personaggio (nome, razza, classe, livello e riferimenti a utente, mondo, sistema)."
    )
    @GetMapping("/modificatori/{id}")
    public ResponseEntity<DatiPersonaggioDTO> modificatoriPersonaggio(
            @Parameter(description = "ID del personaggio", required = true)
            @PathVariable Integer id
    ) {
        DatiPersonaggioDTO result = personaggioService.getDatiPersonaggio(id);

        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Aggiorna i punti ferita",
            description = "Aggiorna i punti ferita"
    )
    @PostMapping("/update-hp")
    public ResponseEntity<Boolean> updateHp(
            @Parameter(description = "Aggiorna i punti ferita", required = true)
            @Valid @RequestBody UpdateHPRequest request
    ) {
        Boolean resp = personaggioService.updateHP(request);
        return ResponseEntity.ok(resp);
    }

    @Operation(
            summary = "Aggiorna i punti ferita",
            description = "Aggiorna i punti ferita"
    )
    @PostMapping("/update-counter")
    public ResponseEntity<Boolean> updateCounter(
            @Parameter(description = "Aggiorna i punti ferita", required = true)
            @Valid @RequestBody UpdateCounterRequest request
    ) {
        Boolean resp = personaggioService.updateContatore(request);
        return ResponseEntity.ok(resp);
    }


}
