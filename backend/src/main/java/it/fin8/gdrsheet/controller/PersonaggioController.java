package it.fin8.gdrsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.dto.*;
import it.fin8.gdrsheet.entity.Personaggio;
import it.fin8.gdrsheet.mapper.ItemMapper;
import it.fin8.gdrsheet.repository.PersonaggioRepository;
import it.fin8.gdrsheet.service.ModificatoriService;
import it.fin8.gdrsheet.service.PersonaggioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/personaggi")
public class PersonaggioController {

    private final PersonaggioRepository repo;

    private final PersonaggioService personaggioService;
    private final ItemMapper itemMapper;
    private final ModificatoriService modificatoriService;

    public PersonaggioController(PersonaggioRepository repo, PersonaggioService personaggioService, ItemMapper itemMapper, ModificatoriService modificatoriService) {
        this.repo = repo;
        this.personaggioService = personaggioService;
        this.itemMapper = itemMapper;
        this.modificatoriService = modificatoriService;
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

    @Operation(
            summary = "Aggiorna i prepared degli incantesimi",
            description = "Aggiorna il numero di prepared per ciascun incantesimo (spellId -> nprepared) di un personaggio, filtrando per classe e livello."
    )
    @PostMapping("stat/update-base")
    public ResponseEntity<Void> updataBaseStatValue(@Valid @RequestBody UpdateBaseStatValueRequest req) {

        // Delego alla service (implementa la logica di persistenza lato tuo ItemService)
        personaggioService.updateBaseStatValue(req);

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "A partire dall'id di un item Livello restituisce il personaggio associato",
            description = "A partire dall'id di un item Livello restituisce il personaggio associato"
    )
    @GetMapping("/id-personaggio-da-livello/{id}")
    public ResponseEntity<Integer> getPersonaggioByLivello(
            @Parameter(description = "ID livello", required = true)
            @PathVariable Integer id
    ) {
        Integer result = personaggioService.getPersonaggioIdDaLivello(id);

        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Restituisce le statistiche associabili a un personaggio e i suoi valori giá presenti",
            description = "Restituisce le statistiche associabili a un personaggio e i suoi valori giá presenti"
    )
    @GetMapping("/stats/{id}")
    public ResponseEntity<List<AbilitaDTO>> getStatByPersonaggio(
            @Parameter(description = "ID Personaggio", required = true)
            @PathVariable Integer id
    ) {
        List<AbilitaDTO> result = personaggioService.getStatsDaPersonaggio(id);

        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Restituisce le classi associabili a un personaggio",
            description = "Restituisce le classi associabili a un personaggio"
    )
    @GetMapping("/classi-associabili/{id}")
    public ResponseEntity<List<ItemDTO>> getClassiAssociabiliPersonaggio(
            @Parameter(description = "ID Personaggio", required = true)
            @PathVariable Integer id
    ) {
        List<ItemDTO> result = personaggioService.getItemAssociabili(id, TipoItem.CLASSE).stream().map(itemMapper::toDTO).toList();

        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Restituisce le classi associabili a un personaggio",
            description = "Restituisce le classi associabili a un personaggio"
    )
    @GetMapping("/maledizioni-associabili/{id}")
    public ResponseEntity<List<ItemDTO>> getMaledizioniAssociabiliPersonaggio(
            @Parameter(description = "ID Personaggio", required = true)
            @PathVariable Integer id
    ) {
        List<ItemDTO> result = personaggioService.getItemAssociabili(id, TipoItem.MALEDIZIONE).stream().map(itemMapper::toDTO).toList();

        return ResponseEntity.ok(result);
    }


    @Operation(
            summary = "Restituisce le classi associabili a un personaggio",
            description = "Restituisce le classi associabili a un personaggio"
    )
    @GetMapping("/abilita-classe/{idPersonaggio}/{livello}/{idClasse}")
    public ResponseEntity<List<AbilitaClasseDTO>> getAbilitaClassePerClassePersonaggio(
            @Parameter(description = "ID Personaggio", required = true)
            @PathVariable Integer idPersonaggio,
            @Parameter(description = "Livello", required = true)
            @PathVariable Integer livello,
            @Parameter(description = "ID Classe", required = true)
            @PathVariable Integer idClasse
    ) {
        List<AbilitaClasseDTO> result = modificatoriService.getAbilitaClasse(idPersonaggio, livello, idClasse);


        return ResponseEntity.ok(result);
    }


}
