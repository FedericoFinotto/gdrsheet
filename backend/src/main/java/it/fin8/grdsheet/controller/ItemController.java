package it.fin8.grdsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import it.fin8.grdsheet.def.TipoItem;
import it.fin8.grdsheet.entity.Item;
import it.fin8.grdsheet.repository.ItemRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/item")
public class ItemController {

    private final ItemRepository repo;

    public ItemController(ItemRepository repo) {
        this.repo = repo;
    }

    @Operation(
            summary = "Recupera la lista di Oggetti per tipo",
            description = "Recupera la lista di Oggetti per tipo"
    )
    @GetMapping("/list/{tipo}")
    public ResponseEntity<List<Item>> getByTipo(
            @Parameter(description = "Tipo Item", required = true)
            @PathVariable TipoItem tipo
    ) {
        List<Item> itms = repo.findItemsByTipo(tipo);

        return ResponseEntity.ok(itms);
    }

    @Operation(
            summary = "Recupera la lista di Oggetti per tipo",
            description = "Recupera la lista di Oggetti per tipo"
    )
    @GetMapping("/{id}")
    public ResponseEntity<Item> getById(
            @Parameter(description = "Id Item", required = true)
            @PathVariable Integer id
    ) {
        Item itm = repo.findItemById(id);

        return ResponseEntity.ok(itm);
    }
}
