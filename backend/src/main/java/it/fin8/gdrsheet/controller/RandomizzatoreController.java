package it.fin8.gdrsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import it.fin8.gdrsheet.dto.ItemTagDTO;
import it.fin8.gdrsheet.dto.RandomizzatoreDTO;
import it.fin8.gdrsheet.entity.Item;
import it.fin8.gdrsheet.entity.Utente;
import it.fin8.gdrsheet.repository.ItemRepository;
import it.fin8.gdrsheet.service.AuthzService;
import it.fin8.gdrsheet.service.ItemTagService;
import it.fin8.gdrsheet.service.RandomizzatoreService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/randomizzatore")
public class RandomizzatoreController {

    private final RandomizzatoreService randomizzatoreService;
    private final ItemTagService itemTagService;
    private final AuthzService authzService;
    private final ItemRepository itemRepository;

    public RandomizzatoreController(RandomizzatoreService randomizzatoreService,
                                    ItemTagService itemTagService,
                                    AuthzService authzService,
                                    ItemRepository itemRepository) {
        this.randomizzatoreService = randomizzatoreService;
        this.itemTagService = itemTagService;
        this.authzService = authzService;
        this.itemRepository = itemRepository;
    }

    @Operation(
            summary = "Tutte le categorie con i loro tag",
            description = "Usato dall'editor dei tag di un oggetto e dall'editor del randomizzatore"
    )
    @GetMapping("/categorie")
    public ResponseEntity<List<RandomizzatoreDTO.CategoriaDTO>> getCategorie() {
        return ResponseEntity.ok(randomizzatoreService.getCategorie());
    }

    @Operation(
            summary = "Elenco dei randomizzatori",
            description = "Per scegliere quali randomizzatori innescare quando un oggetto viene estratto"
    )
    @GetMapping("/elenco")
    public ResponseEntity<List<RandomizzatoreDTO.RefDTO>> getElenco() {
        return ResponseEntity.ok(randomizzatoreService.getElenco());
    }

    @Operation(
            summary = "Configurazione di un randomizzatore",
            description = "Categorie da scegliere (con i relativi tag) e categorie richieste come presenti"
    )
    @GetMapping("/{id}/config")
    public ResponseEntity<RandomizzatoreDTO.ConfigDTO> getConfig(
            @Parameter(description = "Id item randomizzatore", required = true)
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(randomizzatoreService.getConfig(id));
    }

    @Operation(
            summary = "Esegue un'estrazione",
            description = "Estrae un oggetto pesato tra quelli che hanno tutti i tag scelti e almeno "
                    + "un tag per ogni categoria richiesta, poi estrae un tag pesato per ognuna di "
                    + "quelle categorie. L'esito viene registrato nello storico."
    )
    @PostMapping("/{id}/estrai")
    public ResponseEntity<RandomizzatoreDTO.EsitoDTO> estrai(
            @PathVariable Integer id,
            @RequestBody RandomizzatoreDTO.EstrazioneRequest request,
            @AuthenticationPrincipal Utente utente
    ) {
        return ResponseEntity.ok(randomizzatoreService.estrai(id, request, utente));
    }

    @Operation(
            summary = "Storico delle estrazioni di un randomizzatore",
            description = "Dalla più recente. Lo storico è potato automaticamente alle ultime N estrazioni."
    )
    @GetMapping("/{id}/storico")
    public ResponseEntity<List<RandomizzatoreDTO.StoricoDTO>> getStorico(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "5") int limite
    ) {
        return ResponseEntity.ok(randomizzatoreService.getStorico(id, limite));
    }

    @Operation(
            summary = "Tag pesati associati a un oggetto",
            description = "Righe (tag, categoria, peso) usate dai randomizzatori"
    )
    @GetMapping("/item/{idItem}/tag")
    public ResponseEntity<List<ItemTagDTO>> getTags(@PathVariable Integer idItem) {
        return ResponseEntity.ok(itemTagService.getTags(idItem));
    }

    @Operation(
            summary = "Imposta i tag pesati di un oggetto",
            description = "Stato completo: sostituisce integralmente i tag esistenti. "
                    + "Riservato al master del mondo dell'item (o a un admin)."
    )
    @PostMapping("/item/{idItem}/tag")
    public ResponseEntity<List<ItemTagDTO>> setTags(
            @PathVariable Integer idItem,
            @RequestBody ItemTagDTO.UpdateRequest request,
            @AuthenticationPrincipal Utente utente
    ) {
        // stessa regola di ItemController.deleteItem: master del mondo dell'item; se l'item non
        // appartiene a un mondo serve un vero admin, perché nessun master-di-un-mondo ha titolo
        // su contenuto condiviso tra più mondi.
        Item itm = itemRepository.findItemById(idItem);
        if (itm == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item non trovato");
        Integer mondoId = itm.getMondo() != null ? itm.getMondo().getId() : null;
        boolean autorizzato = mondoId != null
                ? authzService.isMasterMondo(utente, mondoId)
                : authzService.isAdmin(utente);
        if (!autorizzato)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo il master di questo mondo (o un admin) può modificare i tag");
        return ResponseEntity.ok(itemTagService.setTags(idItem, request.getTags()));
    }
}
