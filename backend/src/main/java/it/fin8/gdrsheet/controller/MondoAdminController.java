package it.fin8.gdrsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.fin8.gdrsheet.def.TipoPermessoMondo;
import it.fin8.gdrsheet.dto.AddMasterMondoRequest;
import it.fin8.gdrsheet.dto.CreateMondoRequest;
import it.fin8.gdrsheet.dto.CreateSistemaRequest;
import it.fin8.gdrsheet.dto.MasterMondoDTO;
import it.fin8.gdrsheet.dto.MondoDTO;
import it.fin8.gdrsheet.dto.UpdateMondoRequest;
import it.fin8.gdrsheet.entity.Mondo;
import it.fin8.gdrsheet.entity.PermessiMondo;
import it.fin8.gdrsheet.entity.Sistema;
import it.fin8.gdrsheet.entity.Utente;
import it.fin8.gdrsheet.repository.MondoRepository;
import it.fin8.gdrsheet.repository.PermessiMondoRepository;
import it.fin8.gdrsheet.repository.SistemaRepository;
import it.fin8.gdrsheet.repository.UtenteRepository;
import it.fin8.gdrsheet.service.AuthzService;
import it.fin8.gdrsheet.service.PartyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestione del permesso Master-per-mondo (tabella permessi_mondo): chi ADMIN concede/revoca a
 * un utente la gestione del compendio di un mondo specifico. Riservato agli admin — assegnare
 * questo permesso è una decisione "chi comanda dove", non qualcosa che un master delega da solo
 * (a differenza del master di un party, che può aggiungere altri membri al proprio party).
 */
@RestController
@RequestMapping("/api/mondo")
public class MondoAdminController {

    private final MondoRepository mondoRepository;
    private final SistemaRepository sistemaRepository;
    private final PermessiMondoRepository permessiMondoRepository;
    private final UtenteRepository utenteRepository;
    private final AuthzService authzService;
    private final PartyService partyService;

    public MondoAdminController(MondoRepository mondoRepository, SistemaRepository sistemaRepository,
                                PermessiMondoRepository permessiMondoRepository,
                                UtenteRepository utenteRepository, AuthzService authzService,
                                PartyService partyService) {
        this.mondoRepository = mondoRepository;
        this.sistemaRepository = sistemaRepository;
        this.permessiMondoRepository = permessiMondoRepository;
        this.utenteRepository = utenteRepository;
        this.authzService = authzService;
        this.partyService = partyService;
    }

    private MondoDTO toDTO(Mondo m) {
        return new MondoDTO(m.getId(), m.getDescrizione(),
                m.getSistema() != null ? m.getSistema().getId() : null,
                m.getSistema() != null ? m.getSistema().getDescrizione() : null);
    }

    private void assertAdmin(Utente utente) {
        if (!authzService.isAdmin(utente))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Riservato agli admin");
    }

    /**
     * Mondi tra cui l'utente loggato può switchare: per un admin (in modalità admin) tutti i
     * mondi esistenti; per chiunque altro, l'unione di quelli di cui è master (permessi_mondo) e
     * di quelli a cui accede semplicemente come membro di un party (getMieiMondi) — altrimenti un
     * giocatore normale (nessun permesso_mondo) non avrebbe MAI un "mondo corrente", e con lui
     * fallirebbero anche i picker che dipendono da esso (es. VisibilitaPicker, party giocanti).
     * Usato anche dal menu per decidere se mostrare lo switcher vero e proprio (ha senso solo con
     * 2+ mondi disponibili: un semplice giocatore in un solo mondo non lo vede, ma "corrente"
     * resta comunque valorizzato).
     */
    @Operation(summary = "Mondi tra cui l'utente loggato può switchare (o comunque il suo mondo corrente)")
    @GetMapping("/disponibili")
    public ResponseEntity<List<MondoDTO>> getDisponibili(@AuthenticationPrincipal Utente utente) {
        if (authzService.isAdmin(utente)) {
            List<MondoDTO> tutti = mondoRepository.findAll().stream().map(this::toDTO)
                    .sorted((a, b) -> a.descrizione().compareToIgnoreCase(b.descrizione()))
                    .toList();
            return ResponseEntity.ok(tutti);
        }
        Map<Integer, MondoDTO> perId = new LinkedHashMap<>();
        for (PermessiMondo pm : permessiMondoRepository.findAllByIdUtente_IdAndPermesso(utente.getId(), TipoPermessoMondo.MASTER)) {
            perId.put(pm.getIdMondo().getId(), toDTO(pm.getIdMondo()));
        }
        for (MondoDTO m : partyService.getMieiMondi(utente)) {
            perId.putIfAbsent(m.id(), m);
        }
        List<MondoDTO> result = perId.values().stream()
                .sorted((a, b) -> a.descrizione().compareToIgnoreCase(b.descrizione()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Tutti i mondi (admin)", description = "Per la UI di gestione dei permessi per mondo.")
    @GetMapping
    public ResponseEntity<List<MondoDTO>> getAll(@AuthenticationPrincipal Utente utente) {
        assertAdmin(utente);
        List<MondoDTO> result = mondoRepository.findAll().stream().map(this::toDTO)
                .sorted((a, b) -> a.descrizione().compareToIgnoreCase(b.descrizione()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Tutti i sistemi (admin)")
    @GetMapping("/sistemi")
    public ResponseEntity<List<MondoDTO>> getSistemi(@AuthenticationPrincipal Utente utente) {
        assertAdmin(utente);
        List<MondoDTO> result = sistemaRepository.findAll().stream()
                .map(s -> new MondoDTO(s.getId(), s.getDescrizione(), null, null))
                .sorted((a, b) -> a.descrizione().compareToIgnoreCase(b.descrizione()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Crea un nuovo sistema", description = "Admin.")
    @PostMapping("/sistemi")
    public ResponseEntity<MondoDTO> creaSistema(@Valid @RequestBody CreateSistemaRequest req,
                                                @AuthenticationPrincipal Utente utente) {
        assertAdmin(utente);
        Sistema s = new Sistema();
        s.setDescrizione(req.getDescrizione().trim());
        s = sistemaRepository.save(s);
        return ResponseEntity.ok(new MondoDTO(s.getId(), s.getDescrizione(), null, null));
    }

    @Operation(summary = "Crea un nuovo mondo", description = "Admin. sistemaId obbligatorio (un mondo appartiene sempre a un sistema).")
    @PostMapping
    public ResponseEntity<MondoDTO> creaMondo(@Valid @RequestBody CreateMondoRequest req,
                                              @AuthenticationPrincipal Utente utente) {
        assertAdmin(utente);
        Sistema sistema = sistemaRepository.findById(req.getSistemaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sistema non trovato"));
        Mondo m = new Mondo();
        m.setDescrizione(req.getDescrizione().trim());
        m.setSistema(sistema);
        m = mondoRepository.save(m);
        return ResponseEntity.ok(toDTO(m));
    }

    @Operation(summary = "Aggiorna un mondo", description = "Admin. Rinomina e/o riassegna il sistema (entrambi opzionali: null = non modificare).")
    @PutMapping("/{mondoId}")
    public ResponseEntity<MondoDTO> aggiornaMondo(@PathVariable Integer mondoId, @RequestBody UpdateMondoRequest req,
                                                  @AuthenticationPrincipal Utente utente) {
        assertAdmin(utente);
        Mondo m = mondoRepository.findById(mondoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mondo non trovato"));
        if (req.getDescrizione() != null && !req.getDescrizione().isBlank()) m.setDescrizione(req.getDescrizione().trim());
        if (req.getSistemaId() != null) {
            Sistema sistema = sistemaRepository.findById(req.getSistemaId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sistema non trovato"));
            m.setSistema(sistema);
        }
        m = mondoRepository.save(m);
        return ResponseEntity.ok(toDTO(m));
    }

    @Operation(summary = "Master di un mondo", description = "Utenti con permesso MASTER su questo mondo (admin).")
    @GetMapping("/{mondoId}/master")
    public ResponseEntity<List<MasterMondoDTO>> getMaster(@PathVariable Integer mondoId,
                                                          @AuthenticationPrincipal Utente utente) {
        assertAdmin(utente);
        List<MasterMondoDTO> result = permessiMondoRepository.findAllByIdMondo_Id(mondoId).stream()
                .filter(pm -> TipoPermessoMondo.MASTER.equals(pm.getPermesso()))
                .map(pm -> new MasterMondoDTO(pm.getIdUtente().getId(), pm.getIdUtente().getUsername(), pm.getIdUtente().getName()))
                .sorted((a, b) -> a.getUsername().compareToIgnoreCase(b.getUsername()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Rende un utente master di un mondo", description = "Admin.")
    @PostMapping("/{mondoId}/master")
    public ResponseEntity<MasterMondoDTO> addMaster(@PathVariable Integer mondoId,
                                                    @Valid @RequestBody AddMasterMondoRequest req,
                                                    @AuthenticationPrincipal Utente utente) {
        assertAdmin(utente);
        Mondo mondo = mondoRepository.findById(mondoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mondo non trovato"));
        Utente target = utenteRepository.findByUsernameIgnoreCase(req.getUsername().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utente non trovato"));

        if (permessiMondoRepository.existsByIdUtente_IdAndIdMondo_IdAndPermesso(target.getId(), mondoId, TipoPermessoMondo.MASTER))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'utente è già master di questo mondo");

        PermessiMondo pm = new PermessiMondo();
        pm.setIdUtente(target);
        pm.setIdMondo(mondo);
        pm.setPermesso(TipoPermessoMondo.MASTER);
        permessiMondoRepository.save(pm);

        return ResponseEntity.ok(new MasterMondoDTO(target.getId(), target.getUsername(), target.getName()));
    }

    @Operation(summary = "Revoca il permesso master di un mondo a un utente", description = "Admin.")
    @DeleteMapping("/{mondoId}/master/{utenteId}")
    public ResponseEntity<Void> removeMaster(@PathVariable Integer mondoId, @PathVariable Integer utenteId,
                                             @AuthenticationPrincipal Utente utente) {
        assertAdmin(utente);
        permessiMondoRepository.deleteByIdUtente_IdAndIdMondo_IdAndPermesso(utenteId, mondoId, TipoPermessoMondo.MASTER);
        return ResponseEntity.noContent().build();
    }
}
