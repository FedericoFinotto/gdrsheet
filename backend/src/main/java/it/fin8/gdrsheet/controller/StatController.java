package it.fin8.gdrsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.fin8.gdrsheet.entity.Stat;
import it.fin8.gdrsheet.repository.StatRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
public class StatController {

    private final StatRepository statRepository;

    public StatController(StatRepository statRepository) {
        this.statRepository = statRepository;
    }

    @Operation(
            summary = "Lista delle stat",
            description = "Tutte le stat disponibili (id, tipo, label), ordinate per label"
    )
    @GetMapping
    public ResponseEntity<List<Stat>> getAll() {
        return ResponseEntity.ok(statRepository.findAllByOrderByLabelAsc());
    }
}
