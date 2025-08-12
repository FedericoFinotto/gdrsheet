package it.fin8.gdrsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import it.fin8.gdrsheet.dto.SpellBookIncantesimoDTO;
import it.fin8.gdrsheet.dto.UpdatePreparedRequest;
import it.fin8.gdrsheet.dto.UpdateSpellUsageRequest;
import it.fin8.gdrsheet.entity.Item;
import it.fin8.gdrsheet.repository.ItemRepository;
import it.fin8.gdrsheet.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/item")
public class ItemController {

    private final ItemRepository repo;
    private final ItemService itemService;

    public ItemController(ItemRepository repo, ItemService itemService) {
        this.repo = repo;
        this.itemService = itemService;
    }

    @Operation(
            summary = "Recupera un item Specifico",
            description = "Recupera un item Specifico"
    )
    @GetMapping("/{id}")
    public ResponseEntity<Item> getById(
            @Parameter(description = "Id Item", required = true)
            @PathVariable Integer id
    ) {
        Item itm = repo.findItemById(id);

        return ResponseEntity.ok(itm);
    }

    @Operation(
            summary = "Recupera una lista di incantesimi per classe e livello",
            description = "Recupera una lista di incantesimi per classe e livello"
    )
    @GetMapping("incantesimi/{idClasse}/{livello}")
    public ResponseEntity<List<SpellBookIncantesimoDTO>> getById(
            @Parameter(description = "Id Classe", required = true)
            @PathVariable Integer idClasse,
            @Parameter(description = "Livello", required = true)
            @PathVariable Integer livello
    ) {
        List<SpellBookIncantesimoDTO> result = itemService.getListIncantesimiByClasseAndLevel(idClasse, livello);

        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Abilita/Disabilita Item",
            description = "Abilita/Disabilita Item"
    )
    @GetMapping("switch-state/{id}")
    public ResponseEntity<Item> switchItemState(
            @Parameter(description = "Id Item", required = true)
            @PathVariable Integer id
    ) {
        Item itm = itemService.switchItemState(id);

        return ResponseEntity.ok(itm);
    }

    @Operation(
            summary = "Aggiorna i prepared degli incantesimi",
            description = "Aggiorna il numero di prepared per ciascun incantesimo (spellId -> nprepared) di un personaggio, filtrando per classe e livello."
    )
    @PostMapping("incantesimi/prepara")
    public ResponseEntity<Void> updatePrepared(@Valid @RequestBody UpdatePreparedRequest req) {
        // Validazione minima
        if (req == null || req.getIdPersonaggio() == null || req.getPrepared() == null) {
            return ResponseEntity.badRequest().build();
        }

        // Delego alla service (implementa la logica di persistenza lato tuo ItemService)
        itemService.updatePreparedForCharacterAndLevel(req);

        return ResponseEntity.noContent().build(); // 204
    }

    @Operation(
            summary = "Aggiorna i prepared degli incantesimi",
            description = "Aggiorna il numero di prepared per ciascun incantesimo (spellId -> nprepared) di un personaggio, filtrando per classe e livello."
    )
    @PostMapping("incantesimi/update-spellusage")
    public ResponseEntity<Void> updateSpellUsage(@Valid @RequestBody UpdateSpellUsageRequest req) {

        // Delego alla service (implementa la logica di persistenza lato tuo ItemService)
        itemService.updateSpellUsage(req);

        return ResponseEntity.noContent().build();
    }
}
