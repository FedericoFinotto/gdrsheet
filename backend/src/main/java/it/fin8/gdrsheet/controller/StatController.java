package it.fin8.gdrsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.fin8.gdrsheet.StatDefault;
import it.fin8.gdrsheet.def.TipoStat;
import it.fin8.gdrsheet.dto.MondoDTO;
import it.fin8.gdrsheet.dto.StatDefaultDTO;
import it.fin8.gdrsheet.dto.StatRequest;
import it.fin8.gdrsheet.entity.Mondo;
import it.fin8.gdrsheet.entity.Stat;
import it.fin8.gdrsheet.entity.Utente;
import it.fin8.gdrsheet.repository.MondoRepository;
import it.fin8.gdrsheet.repository.StatDefaultRepository;
import it.fin8.gdrsheet.repository.StatRepository;
import it.fin8.gdrsheet.service.AuthzService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
public class StatController {

    private final StatRepository statRepository;
    private final StatDefaultRepository statDefaultRepository;
    private final MondoRepository mondoRepository;
    private final AuthzService authzService;

    public StatController(StatRepository statRepository, StatDefaultRepository statDefaultRepository,
                          MondoRepository mondoRepository, AuthzService authzService) {
        this.statRepository = statRepository;
        this.statDefaultRepository = statDefaultRepository;
        this.mondoRepository = mondoRepository;
        this.authzService = authzService;
    }

    private void assertMasterOrAdmin(Utente utente) {
        if (!authzService.isMasterOrAdmin(utente))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Riservato a master e admin");
    }

    @Operation(summary = "Lista delle stat", description = "Tutte le stat disponibili (id, tipo, label), ordinate per label")
    @GetMapping
    public ResponseEntity<List<Stat>> getAll() {
        return ResponseEntity.ok(statRepository.findAllByOrderByLabelAsc());
    }

    @Operation(summary = "Crea/aggiorna una stat", description = "Riservato a master e admin")
    @PostMapping
    public ResponseEntity<Stat> createStat(@RequestBody StatRequest req, @AuthenticationPrincipal Utente utente) {
        assertMasterOrAdmin(utente);
        if (req.getId() == null || req.getId().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id obbligatorio");
        if (req.getLabel() == null || req.getLabel().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Label obbligatoria");
        TipoStat tipo;
        try {
            tipo = TipoStat.valueOf(req.getTipo());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo stat non valido");
        }
        Stat s = statRepository.findById(req.getId().trim()).orElseGet(Stat::new);
        s.setId(req.getId().trim());
        s.setTipo(tipo);
        s.setLabel(req.getLabel().trim());
        return ResponseEntity.ok(statRepository.save(s));
    }

    @Operation(summary = "Mondi disponibili", description = "Tutti i mondi (per associare le stat_default). Master/admin")
    @GetMapping("/mondi")
    public ResponseEntity<List<MondoDTO>> getMondi(@AuthenticationPrincipal Utente utente) {
        assertMasterOrAdmin(utente);
        List<MondoDTO> result = mondoRepository.findAll().stream()
                .map(m -> new MondoDTO(m.getId(), m.getDescrizione(), null, null))
                .toList();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "stat_default di un mondo", description = "Master/admin")
    @GetMapping("/default/{mondoId}")
    public ResponseEntity<List<StatDefaultDTO>> getDefaults(@PathVariable Integer mondoId,
                                                            @AuthenticationPrincipal Utente utente) {
        assertMasterOrAdmin(utente);
        List<StatDefaultDTO> result = statDefaultRepository.findAllByMondo_Id(mondoId).stream()
                .map(this::toDTO).toList();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Crea una stat_default per un mondo", description = "Master/admin")
    @PostMapping("/default")
    public ResponseEntity<StatDefaultDTO> createDefault(@RequestBody StatDefaultDTO req,
                                                        @AuthenticationPrincipal Utente utente) {
        assertMasterOrAdmin(utente);
        if (req.getMondoId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mondo obbligatorio");
        if (req.getStatId() == null || req.getStatId().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stat obbligatoria");
        Mondo mondo = mondoRepository.findById(req.getMondoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mondo non trovato"));
        if (mondo.getSistema() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Il mondo non ha un sistema associato");

        StatDefault sd = new StatDefault();
        sd.setMondo(mondo);
        sd.setSistema(mondo.getSistema());
        sd.setStatId(req.getStatId().trim());
        sd.setValoreDefault(req.getValoreDefault());
        if (req.getDefaultModId() != null && !req.getDefaultModId().isBlank()) {
            sd.setDefaultMod(statRepository.findById(req.getDefaultModId().trim()).orElse(null));
        }
        sd.setAddestramento(Boolean.TRUE.equals(req.getAddestramento()));
        return ResponseEntity.ok(toDTO(statDefaultRepository.save(sd)));
    }

    @Operation(summary = "Aggiorna una stat_default", description = "Aggiorna valore default, modificatore e addestramento. Master/admin")
    @PutMapping("/default/{id}")
    public ResponseEntity<StatDefaultDTO> updateDefault(@PathVariable Integer id, @RequestBody StatDefaultDTO req,
                                                        @AuthenticationPrincipal Utente utente) {
        assertMasterOrAdmin(utente);
        StatDefault sd = statDefaultRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "stat_default non trovata"));
        sd.setValoreDefault(req.getValoreDefault());
        if (req.getDefaultModId() != null && !req.getDefaultModId().isBlank()) {
            sd.setDefaultMod(statRepository.findById(req.getDefaultModId().trim()).orElse(null));
        } else {
            sd.setDefaultMod(null);
        }
        sd.setAddestramento(Boolean.TRUE.equals(req.getAddestramento()));
        return ResponseEntity.ok(toDTO(statDefaultRepository.save(sd)));
    }

    @Operation(summary = "Elimina una stat_default", description = "Master/admin")
    @DeleteMapping("/default/{id}")
    public ResponseEntity<Void> deleteDefault(@PathVariable Integer id, @AuthenticationPrincipal Utente utente) {
        assertMasterOrAdmin(utente);
        statDefaultRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private StatDefaultDTO toDTO(StatDefault sd) {
        Stat stat = sd.getStatId() != null ? statRepository.findById(sd.getStatId()).orElse(null) : null;
        return new StatDefaultDTO(
                sd.getId(),
                sd.getMondo() != null ? sd.getMondo().getId() : null,
                sd.getStatId(),
                stat != null ? stat.getLabel() : sd.getStatId(),
                sd.getValoreDefault(),
                sd.getDefaultMod() != null ? sd.getDefaultMod().getId() : null,
                sd.getDefaultMod() != null ? sd.getDefaultMod().getLabel() : null,
                sd.getAddestramento()
        );
    }
}
