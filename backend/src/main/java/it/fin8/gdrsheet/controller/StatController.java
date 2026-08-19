package it.fin8.gdrsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.fin8.gdrsheet.StatDefault;
import it.fin8.gdrsheet.def.TipoPermessoMondo;
import it.fin8.gdrsheet.def.TipoStat;
import it.fin8.gdrsheet.dto.MondoDTO;
import it.fin8.gdrsheet.dto.StatDefaultDTO;
import it.fin8.gdrsheet.dto.StatLivelloClasseDTO;
import it.fin8.gdrsheet.dto.StatRequest;
import it.fin8.gdrsheet.entity.Mondo;
import it.fin8.gdrsheet.entity.PermessiMondo;
import it.fin8.gdrsheet.entity.Stat;
import it.fin8.gdrsheet.entity.Utente;
import it.fin8.gdrsheet.repository.MondoRepository;
import it.fin8.gdrsheet.repository.PermessiMondoRepository;
import it.fin8.gdrsheet.repository.StatDefaultRepository;
import it.fin8.gdrsheet.repository.StatRepository;
import it.fin8.gdrsheet.service.AuthzService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/stats")
public class StatController {

    private final StatRepository statRepository;
    private final StatDefaultRepository statDefaultRepository;
    private final MondoRepository mondoRepository;
    private final PermessiMondoRepository permessiMondoRepository;
    private final AuthzService authzService;

    public StatController(StatRepository statRepository, StatDefaultRepository statDefaultRepository,
                          MondoRepository mondoRepository, PermessiMondoRepository permessiMondoRepository,
                          AuthzService authzService) {
        this.statRepository = statRepository;
        this.statDefaultRepository = statDefaultRepository;
        this.mondoRepository = mondoRepository;
        this.permessiMondoRepository = permessiMondoRepository;
        this.authzService = authzService;
    }

    /** Il catalogo Stat (FOR/DES/COS/...) è globale, condiviso da ogni mondo: solo un vero admin lo tocca. */
    private void assertAdmin(Utente utente) {
        if (!authzService.isAdmin(utente))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Riservato agli admin");
    }

    /**
     * stat_default è per mondo: la gestisce chi ha il permesso STATS su QUEL mondo (o un admin) —
     * permesso indipendente da MASTER (vedi TipoPermessoMondo), un master non lo ottiene
     * automaticamente.
     */
    private void assertStatsMondo(Utente utente, Integer mondoId) {
        if (!authzService.isStatsMondo(utente, mondoId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Riservato a chi ha il permesso Statistiche su questo mondo");
    }

    @Operation(summary = "Lista delle stat", description = "Tutte le stat disponibili (id, tipo, label), ordinate per label")
    @GetMapping
    public ResponseEntity<List<Stat>> getAll() {
        return ResponseEntity.ok(statRepository.findAllByOrderByLabelAsc());
    }

    @Operation(summary = "Crea/aggiorna una stat", description = "Catalogo globale condiviso da tutti i mondi: riservato agli admin")
    @PostMapping
    public ResponseEntity<Stat> createStat(@RequestBody StatRequest req, @AuthenticationPrincipal Utente utente) {
        assertAdmin(utente);
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
        Optional<Stat> esistente = statRepository.findById(req.getId().trim());
        boolean nuova = esistente.isEmpty();
        Stat s = esistente.orElseGet(Stat::new);
        s.setId(req.getId().trim());
        s.setTipo(tipo);
        s.setLabel(req.getLabel().trim());
        if (req.getRankable() != null) {
            s.setRankable(req.getRankable());
        } else if (nuova) {
            s.setRankable(true);
        }
        return ResponseEntity.ok(statRepository.save(s));
    }

    @Operation(summary = "Mondi disponibili", description = "Tutti i mondi per un admin; solo quelli su cui si ha il permesso Statistiche altrimenti (per associare le stat_default)")
    @GetMapping("/mondi")
    public ResponseEntity<List<MondoDTO>> getMondi(@AuthenticationPrincipal Utente utente) {
        List<Mondo> mondi = authzService.isAdmin(utente)
                ? mondoRepository.findAll()
                : permessiMondoRepository.findAllByIdUtente_IdAndPermesso(utente.getId(), TipoPermessoMondo.STATS).stream()
                        .map(PermessiMondo::getIdMondo).toList();
        List<MondoDTO> result = mondi.stream()
                .map(m -> new MondoDTO(m.getId(), m.getDescrizione(), null, null))
                .toList();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "stat_default di un mondo", description = "Chi ha il permesso Statistiche su quel mondo, o admin")
    @GetMapping("/default/{mondoId}")
    public ResponseEntity<List<StatDefaultDTO>> getDefaults(@PathVariable Integer mondoId,
                                                            @AuthenticationPrincipal Utente utente) {
        assertStatsMondo(utente, mondoId);
        List<StatDefaultDTO> result = statDefaultRepository.findAllByMondo_Id(mondoId).stream()
                .map(this::toDTO).toList();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Stat con colonna nella Tabella Livelli, per un mondo",
            description = "Aperto a qualunque utente autenticato: serve solo a sapere quali colonne mostrare " +
                    "nell'editor classe, non è un'informazione riservata (a differenza di /default/{mondoId})")
    @GetMapping("/livello-classe/{mondoId}")
    public ResponseEntity<List<StatLivelloClasseDTO>> getLivelloClasse(@PathVariable Integer mondoId) {
        List<StatLivelloClasseDTO> result = statDefaultRepository.findAllByMondo_Id(mondoId).stream()
                .filter(sd -> Boolean.TRUE.equals(sd.getLivelloClasse()))
                .map(sd -> {
                    Stat stat = sd.getStatId() != null ? statRepository.findById(sd.getStatId()).orElse(null) : null;
                    return new StatLivelloClasseDTO(sd.getStatId(), stat != null ? stat.getLabel() : sd.getStatId(),
                            sd.getModoLivelloClasse());
                })
                .toList();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Crea una stat_default per un mondo", description = "Chi ha il permesso Statistiche su quel mondo, o admin")
    @PostMapping("/default")
    public ResponseEntity<StatDefaultDTO> createDefault(@RequestBody StatDefaultDTO req,
                                                        @AuthenticationPrincipal Utente utente) {
        if (req.getMondoId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mondo obbligatorio");
        assertStatsMondo(utente, req.getMondoId());
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
        sd.setLivelloClasse(Boolean.TRUE.equals(req.getLivelloClasse()));
        sd.setModoLivelloClasse(sd.getLivelloClasse() ? req.getModoLivelloClasse() : null);
        return ResponseEntity.ok(toDTO(statDefaultRepository.save(sd)));
    }

    @Operation(summary = "Aggiorna una stat_default", description = "Aggiorna valore default, modificatore e addestramento. Chi ha il permesso Statistiche su quel mondo, o admin")
    @PutMapping("/default/{id}")
    public ResponseEntity<StatDefaultDTO> updateDefault(@PathVariable Integer id, @RequestBody StatDefaultDTO req,
                                                        @AuthenticationPrincipal Utente utente) {
        StatDefault sd = statDefaultRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "stat_default non trovata"));
        assertStatsMondo(utente, sd.getMondo() != null ? sd.getMondo().getId() : null);
        sd.setValoreDefault(req.getValoreDefault());
        if (req.getDefaultModId() != null && !req.getDefaultModId().isBlank()) {
            sd.setDefaultMod(statRepository.findById(req.getDefaultModId().trim()).orElse(null));
        } else {
            sd.setDefaultMod(null);
        }
        sd.setAddestramento(Boolean.TRUE.equals(req.getAddestramento()));
        sd.setLivelloClasse(Boolean.TRUE.equals(req.getLivelloClasse()));
        sd.setModoLivelloClasse(sd.getLivelloClasse() ? req.getModoLivelloClasse() : null);
        return ResponseEntity.ok(toDTO(statDefaultRepository.save(sd)));
    }

    @Operation(summary = "Elimina una stat_default", description = "Chi ha il permesso Statistiche su quel mondo, o admin")
    @DeleteMapping("/default/{id}")
    public ResponseEntity<Void> deleteDefault(@PathVariable Integer id, @AuthenticationPrincipal Utente utente) {
        StatDefault sd = statDefaultRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "stat_default non trovata"));
        assertStatsMondo(utente, sd.getMondo() != null ? sd.getMondo().getId() : null);
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
                sd.getAddestramento(),
                sd.getLivelloClasse(),
                sd.getModoLivelloClasse()
        );
    }
}
