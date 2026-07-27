package it.fin8.gdrsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.fin8.gdrsheet.dto.QuestDTO;
import it.fin8.gdrsheet.dto.QuestDettaglioDTO;
import it.fin8.gdrsheet.dto.QuestSceltaDTO;
import it.fin8.gdrsheet.entity.Utente;
import it.fin8.gdrsheet.service.AuthzService;
import it.fin8.gdrsheet.service.ItemService;
import it.fin8.gdrsheet.service.QuestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/quest")
public class QuestController {

    private final QuestService questService;
    private final ItemService itemService;
    private final AuthzService authzService;

    public QuestController(QuestService questService, ItemService itemService, AuthzService authzService) {
        this.questService = questService;
        this.itemService = itemService;
        this.authzService = authzService;
    }

    @Operation(
            summary = "Albero delle quest visibili a un personaggio (proprie + del party + del mondo)",
            description = "archiviate=false (default) mostra solo le non archiviate; true mostra SOLO le archiviate."
    )
    @GetMapping("/personaggio/{id}")
    public ResponseEntity<List<QuestDTO>> getQuestPersonaggio(@PathVariable Integer id,
                                                               @RequestParam(defaultValue = "false") boolean archiviate,
                                                               @AuthenticationPrincipal Utente utente) {
        return ResponseEntity.ok(questService.getQuestPersonaggio(id, utente, archiviate));
    }

    @Operation(
            summary = "Albero delle quest visibili a un party (proprie + del mondo + dei personaggi membri)",
            description = "archiviate=false (default) mostra solo le non archiviate; true mostra SOLO le archiviate."
    )
    @GetMapping("/party/{id}")
    public ResponseEntity<List<QuestDTO>> getQuestParty(@PathVariable Integer id,
                                                         @RequestParam(defaultValue = "false") boolean archiviate,
                                                         @AuthenticationPrincipal Utente utente) {
        return ResponseEntity.ok(questService.getQuestParty(id, utente, archiviate));
    }

    @Operation(
            summary = "Descrizione e note di una singola quest",
            description = "Parte pesante del nodo, esclusa dall'albero e caricata solo quando la quest viene aperta."
    )
    @GetMapping("/{id}/dettaglio")
    public ResponseEntity<QuestDettaglioDTO> getDettaglio(@PathVariable Integer id,
                                                          @AuthenticationPrincipal Utente utente) {
        return ResponseEntity.ok(questService.getDettaglio(id, utente));
    }

    @Operation(
            summary = "Albero completo (radice + sotto-quest) a cui appartiene una quest",
            description = "Risale alla quest radice a qualunque profondità e ne restituisce l'intero albero: usato dalla ricerca profonda per aprire, cliccando su un risultato QUEST, lo stesso componente della pagina Quest."
    )
    @GetMapping("/{id}/albero")
    public ResponseEntity<QuestDTO> getQuestAlbero(@PathVariable Integer id) {
        return ResponseEntity.ok(questService.getQuestAlbero(id));
    }

    @Operation(
            summary = "Quest radice collegabili come sotto-quest",
            description = "Solo quest non già figlie di un'altra quest: per costruzione la scelta non può creare cicli."
    )
    @GetMapping("/search-radici")
    public ResponseEntity<List<QuestSceltaDTO>> searchQuestRadici(
            @RequestParam String q,
            @RequestParam(required = false) Integer excludeId
    ) {
        return ResponseEntity.ok(questService.searchQuestRadici(q, excludeId));
    }

    @Operation(summary = "Inverte lo stato di completamento di una quest foglia")
    @PostMapping("/{id}/toggle")
    public ResponseEntity<Void> toggleCompletata(@PathVariable Integer id) {
        questService.toggleCompletata(id);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Imposta le righe \"In carico\" di una quest",
            description = "Sostituisce integralmente le righe (righe vuote escluse): modifica rapida senza passare dall'editor completo dell'item."
    )
    @PutMapping("/{id}/in-carico")
    public ResponseEntity<Void> setInCarico(@PathVariable Integer id, @RequestBody List<String> valori) {
        questService.setInCarico(id, valori);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Elimina una quest per tutti i giocatori che la vedono",
            description = "Cancella la quest e a cascata tutte le sue sotto-quest, con i relativi " +
                    "collegamenti, modificatori, avanzamenti, permessi e item_label. Gli eventuali " +
                    "figli non-QUEST (item collegati, potenzialmente condivisi) vengono solo scollegati. " +
                    "Riservato a master e admin."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuest(@PathVariable Integer id,
                                            @AuthenticationPrincipal Utente utente) {
        if (!authzService.isMasterOrAdmin(utente))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo master e admin possono eliminare una quest");
        itemService.deleteQuestTree(id);
        return ResponseEntity.noContent().build();
    }
}
