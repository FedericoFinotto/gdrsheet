package it.fin8.grdsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import it.fin8.grdsheet.entity.Item;
import it.fin8.grdsheet.repository.ItemRepository;
import it.fin8.grdsheet.service.ItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
