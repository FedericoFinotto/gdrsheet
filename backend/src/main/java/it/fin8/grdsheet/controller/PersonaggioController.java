package it.fin8.grdsheet.controller;

import it.fin8.grdsheet.entity.Personaggio;
import it.fin8.grdsheet.repository.PersonaggioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/personaggi")
public class PersonaggioController {

    private final PersonaggioRepository repo;

    public PersonaggioController(PersonaggioRepository repo) {
        this.repo = repo;
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
}
