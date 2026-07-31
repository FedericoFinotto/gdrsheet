package it.fin8.gdrsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import it.fin8.gdrsheet.dto.ItemImmagineDTO;
import it.fin8.gdrsheet.entity.Item;
import it.fin8.gdrsheet.entity.ItemImmagine;
import it.fin8.gdrsheet.entity.Utente;
import it.fin8.gdrsheet.repository.ItemImmagineRepository;
import it.fin8.gdrsheet.repository.ItemRepository;
import it.fin8.gdrsheet.service.AuthzService;
import it.fin8.gdrsheet.service.ImageHostClient;
import it.fin8.gdrsheet.service.ItemImmagineService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Immagini degli item. I file vengono caricati sull'host esterno passando da qui, così la API
 * key resta lato server e non finisce nel bundle del frontend.
 */
@RestController
@RequestMapping("/api/immagini")
public class ItemImmagineController {

    private final ItemImmagineService immagineService;
    private final ItemImmagineRepository immagineRepository;
    private final ItemRepository itemRepository;
    private final AuthzService authzService;
    private final ImageHostClient host;

    public ItemImmagineController(ItemImmagineService immagineService,
                                  ItemImmagineRepository immagineRepository,
                                  ItemRepository itemRepository,
                                  AuthzService authzService,
                                  ImageHostClient host) {
        this.immagineService = immagineService;
        this.immagineRepository = immagineRepository;
        this.itemRepository = itemRepository;
        this.authzService = authzService;
        this.host = host;
    }

    @Operation(summary = "Stato dell'integrazione con l'host immagini",
            description = "Il frontend nasconde il caricamento se manca la configurazione")
    @GetMapping("/stato")
    public ResponseEntity<Map<String, Object>> stato() {
        return ResponseEntity.ok(Map.of(
                "configurato", host.isConfigurato(),
                "host", host.nomeHost(),
                "cancellazioneRemota", host.supportaCancellazioneRemota(),
                "maxByte", host.dimensioneMassimaByte()));
    }

    @Operation(summary = "Immagini di un item")
    @GetMapping("/item/{idItem}")
    public ResponseEntity<List<ItemImmagineDTO>> getImmagini(@PathVariable Integer idItem) {
        return ResponseEntity.ok(immagineService.getImmagini(idItem));
    }

    @Operation(summary = "Carica un'immagine sull'host esterno e la collega all'item",
            description = "Riservato al master del mondo dell'item (o a un admin).")
    @PostMapping(value = "/item/{idItem}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ItemImmagineDTO> carica(
            @PathVariable Integer idItem,
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Didascalia opzionale")
            @RequestParam(required = false) String titolo,
            @AuthenticationPrincipal Utente utente
    ) {
        assertPuoModificare(itemRepository.findItemById(idItem), utente);
        return ResponseEntity.ok(immagineService.carica(idItem, file, titolo));
    }

    @Operation(summary = "Rimuove un'immagine dall'item e cancella il file dall'host")
    @DeleteMapping("/{idImmagine}")
    public ResponseEntity<Void> elimina(@PathVariable Integer idImmagine,
                                        @AuthenticationPrincipal Utente utente) {
        ItemImmagine img = immagineRepository.findById(idImmagine)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Immagine non trovata"));
        assertPuoModificare(img.getItem(), utente);
        immagineService.elimina(idImmagine);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Riordina le immagini di un item")
    @PostMapping("/item/{idItem}/ordine")
    public ResponseEntity<List<ItemImmagineDTO>> riordina(@PathVariable Integer idItem,
                                                          @RequestBody List<Integer> idInOrdine,
                                                          @AuthenticationPrincipal Utente utente) {
        assertPuoModificare(itemRepository.findItemById(idItem), utente);
        return ResponseEntity.ok(immagineService.riordina(idItem, idInOrdine));
    }

    /**
     * Stessa regola usata per eliminare o taggare un item: master del mondo a cui appartiene;
     * se l'item non è di nessun mondo serve un admin, perché nessun master-di-un-mondo ha
     * titolo su contenuto condiviso tra più mondi.
     */
    private boolean puoModificare(Item item, Utente utente) {
        if (item == null) return false;
        Integer mondoId = item.getMondo() != null ? item.getMondo().getId() : null;
        return mondoId != null ? authzService.isMasterMondo(utente, mondoId) : authzService.isAdmin(utente);
    }

    private void assertPuoModificare(Item item, Utente utente) {
        if (item == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item non trovato");
        if (!puoModificare(item, utente))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo il master di questo mondo (o un admin) può gestire le immagini");
    }
}
