package it.fin8.gdrsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.fin8.gdrsheet.dto.QuestDTO;
import it.fin8.gdrsheet.entity.Utente;
import it.fin8.gdrsheet.service.QuestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quest")
public class QuestController {

    private final QuestService questService;

    public QuestController(QuestService questService) {
        this.questService = questService;
    }

    @Operation(summary = "Albero delle quest visibili a un personaggio (proprie + del party + del mondo)")
    @GetMapping("/personaggio/{id}")
    public ResponseEntity<List<QuestDTO>> getQuestPersonaggio(@PathVariable Integer id,
                                                               @AuthenticationPrincipal Utente utente) {
        return ResponseEntity.ok(questService.getQuestPersonaggio(id, utente));
    }

    @Operation(summary = "Albero delle quest visibili a un party (proprie + del mondo + dei personaggi membri)")
    @GetMapping("/party/{id}")
    public ResponseEntity<List<QuestDTO>> getQuestParty(@PathVariable Integer id,
                                                         @AuthenticationPrincipal Utente utente) {
        return ResponseEntity.ok(questService.getQuestParty(id, utente));
    }

    @Operation(summary = "Inverte lo stato di completamento di una quest foglia")
    @PostMapping("/{id}/toggle")
    public ResponseEntity<Void> toggleCompletata(@PathVariable Integer id) {
        questService.toggleCompletata(id);
        return ResponseEntity.ok().build();
    }
}
