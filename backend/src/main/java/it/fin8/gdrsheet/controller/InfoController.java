package it.fin8.gdrsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.fin8.gdrsheet.dto.InfoDTO;
import it.fin8.gdrsheet.dto.InfoDettaglioDTO;
import it.fin8.gdrsheet.entity.Utente;
import it.fin8.gdrsheet.service.InfoService;
import it.fin8.gdrsheet.service.ItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/info")
public class InfoController {

    private final InfoService infoService;
    private final ItemService itemService;

    public InfoController(InfoService infoService, ItemService itemService) {
        this.infoService = infoService;
        this.itemService = itemService;
    }

    @Operation(
            summary = "Albero degli INFO visibili a un party (propri + del mondo + dei personaggi membri)",
            description = "archiviate=false (default) mostra solo i non archiviati; true mostra SOLO gli archiviati."
    )
    @GetMapping("/party/{id}")
    public ResponseEntity<List<InfoDTO>> getInfoParty(@PathVariable Integer id,
                                                       @RequestParam(defaultValue = "false") boolean archiviate,
                                                       @AuthenticationPrincipal Utente utente) {
        return ResponseEntity.ok(infoService.getInfoParty(id, utente, archiviate));
    }

    @Operation(
            summary = "Descrizione e note di un singolo INFO",
            description = "Parte pesante del nodo, esclusa dall'albero e caricata solo quando l'INFO viene aperto."
    )
    @GetMapping("/{id}/dettaglio")
    public ResponseEntity<InfoDettaglioDTO> getDettaglio(@PathVariable Integer id,
                                                         @AuthenticationPrincipal Utente utente) {
        return ResponseEntity.ok(infoService.getDettaglio(id, utente));
    }

    @Operation(
            summary = "Elimina un INFO per tutti i giocatori che lo vedono",
            description = "Cancella l'INFO e a cascata tutti i suoi sotto-info, con i relativi " +
                    "collegamenti, modificatori, avanzamenti, permessi e item_label. Disponibile a " +
                    "qualunque giocatore che lo veda, non solo a master/admin (come per le quest)."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInfo(@PathVariable Integer id) {
        itemService.deleteInfoTree(id);
        return ResponseEntity.noContent().build();
    }
}
