package it.fin8.grdsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import it.fin8.grdsheet.dto.DatiPersonaggioDTO;
import it.fin8.grdsheet.entity.Personaggio;
import it.fin8.grdsheet.repository.PersonaggioRepository;
import it.fin8.grdsheet.service.PersonaggioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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

//    @Operation(
//            summary = "Recupera un personaggio per ID",
//            description = "Items legati a un personaggio"
//    )
//    @GetMapping("/items/{id}")
//    public ResponseEntity<List<Item>> test(
//            @Parameter(description = "ID del personaggio", required = true)
//            @PathVariable Integer id
//    ) {
//        List<Item> result = personaggioService.flattenItems(id);
//
//        return ResponseEntity.ok(result);
//    }

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


}
