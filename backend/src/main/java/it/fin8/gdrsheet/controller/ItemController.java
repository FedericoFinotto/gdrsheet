package it.fin8.gdrsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.dto.ItemDTO;
import it.fin8.gdrsheet.dto.SpellBookIncantesimoDTO;
import it.fin8.gdrsheet.dto.UpdateItemRequest;
import it.fin8.gdrsheet.dto.UpdateLivelloRequest;
import it.fin8.gdrsheet.dto.UpdatePreparedRequest;
import it.fin8.gdrsheet.dto.UpdateSpellRequest;
import it.fin8.gdrsheet.dto.UpdateSpellUsageRequest;
import it.fin8.gdrsheet.entity.Item;
import it.fin8.gdrsheet.mapper.ItemMapper;
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
    private final ItemMapper itemMapper;

    public ItemController(ItemRepository repo, ItemService itemService, ItemMapper itemMapper) {
        this.repo = repo;
        this.itemService = itemService;
        this.itemMapper = itemMapper;
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
    @GetMapping("switch-state/{idPersonaggio}/{id}")
    public ResponseEntity<Item> switchItemState(
            @Parameter(description = "Id Item", required = true)
            @PathVariable Integer id,
            @Parameter(description = "Id Personaggio", required = true)
            @PathVariable Integer idPersonaggio
    ) {
        Item itm = itemService.switchItemState(id, idPersonaggio);

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

    @Operation(
            summary = "Aggiorna un incantesimo",
            description = "Aggiorna un incantesimo"
    )
    @PostMapping("/editspell/{id}")
    public ResponseEntity<Item> updateSpell(@PathVariable Integer id,
                                            @RequestBody UpdateSpellRequest dto) {
        // service.updateSpell(id, dto);
        return ResponseEntity.ok(itemService.updateSpell(id, dto));
    }

    @Operation(
            summary = "Cerca item per nome",
            description = "Ricerca per nome (contains, case-insensitive), opzionalmente filtrata per tipo. Max 20 risultati."
    )
    @GetMapping("/search")
    public ResponseEntity<List<ItemDTO>> searchItems(
            @Parameter(description = "Testo da cercare nel nome", required = true)
            @RequestParam String q,
            @Parameter(description = "Tipo item (opzionale)")
            @RequestParam(required = false) TipoItem tipo
    ) {
        List<ItemDTO> result = itemService.searchItems(q, tipo).stream().map(itemMapper::toDTO).toList();
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Crea un nuovo item",
            description = "Crea un nuovo item generico con labels e modificatori"
    )
    @PostMapping("/create")
    public ResponseEntity<Item> createItem(@Valid @RequestBody UpdateItemRequest dto) {
        return ResponseEntity.ok(itemService.createItem(dto));
    }

    @Operation(
            summary = "Aggiorna un item generico",
            description = "Aggiorna nome, descrizione, labels e modificatori di un item"
    )
    @PostMapping("/edit/{id}")
    public ResponseEntity<Item> updateItem(@PathVariable Integer id,
                                           @Valid @RequestBody UpdateItemRequest dto) {
        return ResponseEntity.ok(itemService.updateItem(id, dto));
    }

    @Operation(
            summary = "Elimina un item",
            description = "Item intestati a un personaggio: eliminazione completa. Item di compendio con idPersonaggio: scollegamento dal FromCompendio (ed eliminazione se non più referenziato)."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(
            @Parameter(description = "Id Item", required = true)
            @PathVariable Integer id,
            @Parameter(description = "Id Personaggio (contesto di scollegamento)")
            @RequestParam(required = false) Integer idPersonaggio
    ) {
        itemService.deleteItem(id, idPersonaggio);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Aggiorna un item Livello",
            description = "Aggiorna labels, caratteristiche (BASE), ranghi (RANK) e contenuti concessi di un item LIVELLO"
    )
    @PostMapping("/editlivello/{id}")
    public ResponseEntity<Item> updateLivello(@PathVariable Integer id,
                                              @Valid @RequestBody UpdateLivelloRequest dto) {
        return ResponseEntity.ok(itemService.updateLivello(id, dto));
    }

}
