package it.fin8.gdrsheet.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import it.fin8.gdrsheet.config.Constants;
import it.fin8.gdrsheet.def.CardEditorItem;
import it.fin8.gdrsheet.def.TipoCampoEditor;
import it.fin8.gdrsheet.def.TipoCatalogoIncantesimo;
import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.def.TipoPermessoMondo;
import it.fin8.gdrsheet.dto.AddMasterMondoRequest;
import it.fin8.gdrsheet.dto.CreaValoreCatalogoIncantesimoRequest;
import it.fin8.gdrsheet.dto.CreateListaIncantesimiRequest;
import it.fin8.gdrsheet.dto.CreateMondoRequest;
import it.fin8.gdrsheet.dto.CreateSistemaRequest;
import it.fin8.gdrsheet.dto.MasterMondoDTO;
import it.fin8.gdrsheet.dto.MondoConfigDTO;
import it.fin8.gdrsheet.dto.MondoDTO;
import it.fin8.gdrsheet.dto.TipoItemConfigDTO;
import it.fin8.gdrsheet.dto.UpdateMondoConfigRequest;
import it.fin8.gdrsheet.dto.UpdateMondoRequest;
import it.fin8.gdrsheet.dto.UpdateTipoItemConfigRequest;
import it.fin8.gdrsheet.entity.CatalogoIncantesimo;
import it.fin8.gdrsheet.entity.ListaIncantesimi;
import it.fin8.gdrsheet.entity.Mondo;
import it.fin8.gdrsheet.entity.MondoCatalogoIncantesimoAbilitato;
import it.fin8.gdrsheet.entity.MondoListaIncantesimiAbilitata;
import it.fin8.gdrsheet.entity.MondoTipoItemAbilitato;
import it.fin8.gdrsheet.entity.MondoTipoItemCampo;
import it.fin8.gdrsheet.entity.MondoTipoItemCardAbilitata;
import it.fin8.gdrsheet.entity.MondoTipoItemMeta;
import it.fin8.gdrsheet.entity.PermessiMondo;
import it.fin8.gdrsheet.entity.Sistema;
import it.fin8.gdrsheet.entity.Utente;
import it.fin8.gdrsheet.repository.CatalogoIncantesimoRepository;
import it.fin8.gdrsheet.repository.ListaIncantesimiRepository;
import it.fin8.gdrsheet.repository.MondoCatalogoIncantesimoAbilitatoRepository;
import it.fin8.gdrsheet.repository.MondoListaIncantesimiAbilitataRepository;
import it.fin8.gdrsheet.repository.MondoRepository;
import it.fin8.gdrsheet.repository.MondoTipoItemAbilitatoRepository;
import it.fin8.gdrsheet.repository.MondoTipoItemCampoRepository;
import it.fin8.gdrsheet.repository.MondoTipoItemCardAbilitataRepository;
import it.fin8.gdrsheet.repository.MondoTipoItemMetaRepository;
import it.fin8.gdrsheet.repository.PermessiMondoRepository;
import it.fin8.gdrsheet.repository.SistemaRepository;
import it.fin8.gdrsheet.repository.UtenteRepository;
import it.fin8.gdrsheet.service.AuthzService;
import it.fin8.gdrsheet.service.PartyService;
import jakarta.transaction.Transactional;
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
 * Gestione dei permessi per-mondo (tabella permessi_mondo, vedi {@link TipoPermessoMondo}): chi
 * ADMIN concede/revoca a un utente MASTER (potere pieno), STATS (solo stat_default) o PAGINE
 * (solo configurazione: tipi item, card/campi editor, cataloghi) su un mondo specifico. Riservato
 * agli admin — assegnare un permesso è una decisione "chi comanda dove", non qualcosa che un
 * master delega da solo (a differenza del master di un party, che può aggiungere altri membri al
 * proprio party).
 * <p>
 * Espone anche la configurazione per-mondo di cosa è abilitato (tipi item, liste/domini
 * incantesimi), gestibile da chi ha il permesso PAGINE: vedi {@link MondoConfigDTO}.
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
    private final MondoTipoItemAbilitatoRepository mondoTipoItemAbilitatoRepository;
    private final ListaIncantesimiRepository listaIncantesimiRepository;
    private final MondoListaIncantesimiAbilitataRepository mondoListaIncantesimiAbilitataRepository;
    private final MondoTipoItemCardAbilitataRepository mondoTipoItemCardAbilitataRepository;
    private final MondoTipoItemCampoRepository mondoTipoItemCampoRepository;
    private final MondoTipoItemMetaRepository mondoTipoItemMetaRepository;
    private final CatalogoIncantesimoRepository catalogoIncantesimoRepository;
    private final MondoCatalogoIncantesimoAbilitatoRepository mondoCatalogoIncantesimoAbilitatoRepository;
    private final ObjectMapper objectMapper;

    public MondoAdminController(MondoRepository mondoRepository, SistemaRepository sistemaRepository,
                                PermessiMondoRepository permessiMondoRepository,
                                UtenteRepository utenteRepository, AuthzService authzService,
                                PartyService partyService,
                                MondoTipoItemAbilitatoRepository mondoTipoItemAbilitatoRepository,
                                ListaIncantesimiRepository listaIncantesimiRepository,
                                MondoListaIncantesimiAbilitataRepository mondoListaIncantesimiAbilitataRepository,
                                MondoTipoItemCardAbilitataRepository mondoTipoItemCardAbilitataRepository,
                                MondoTipoItemCampoRepository mondoTipoItemCampoRepository,
                                MondoTipoItemMetaRepository mondoTipoItemMetaRepository,
                                CatalogoIncantesimoRepository catalogoIncantesimoRepository,
                                MondoCatalogoIncantesimoAbilitatoRepository mondoCatalogoIncantesimoAbilitatoRepository,
                                ObjectMapper objectMapper) {
        this.mondoRepository = mondoRepository;
        this.sistemaRepository = sistemaRepository;
        this.permessiMondoRepository = permessiMondoRepository;
        this.utenteRepository = utenteRepository;
        this.authzService = authzService;
        this.partyService = partyService;
        this.mondoTipoItemAbilitatoRepository = mondoTipoItemAbilitatoRepository;
        this.listaIncantesimiRepository = listaIncantesimiRepository;
        this.mondoListaIncantesimiAbilitataRepository = mondoListaIncantesimiAbilitataRepository;
        this.mondoTipoItemCardAbilitataRepository = mondoTipoItemCardAbilitataRepository;
        this.mondoTipoItemCampoRepository = mondoTipoItemCampoRepository;
        this.mondoTipoItemMetaRepository = mondoTipoItemMetaRepository;
        this.catalogoIncantesimoRepository = catalogoIncantesimoRepository;
        this.mondoCatalogoIncantesimoAbilitatoRepository = mondoCatalogoIncantesimoAbilitatoRepository;
        this.objectMapper = objectMapper;
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
     * Configurazione di UN mondo (tipi item abilitati, card/campi dell'editor, catalogo
     * scuole/sottoscuole/descrittori/componenti — le "pagine" del mondo): chi ha il permesso
     * PAGINE su QUEL mondo, o un admin. Permesso indipendente da MASTER (vedi TipoPermessoMondo):
     * un master non lo ottiene automaticamente. Creare mondi/sistemi e assegnare/revocare
     * permessi restano invece riservati agli admin (vedi assertAdmin sopra): "chi comanda dove"
     * non è qualcosa che si delega da soli.
     */
    private void assertPagineMondo(Utente utente, Integer mondoId) {
        if (!authzService.isPagineMondo(utente, mondoId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Riservato a chi ha il permesso Pagine su questo mondo");
    }

    /**
     * Lettura di un catalogo GLOBALE (liste/domini incantesimi, scuole/sottoscuole/...): chiunque
     * abbia un permesso su almeno un mondo (MASTER, STATS o PAGINE indifferentemente) deve poterlo
     * leggere per scegliere cosa abilitare nel proprio mondo, anche se non coinvolto in questa
     * chiamata (il catalogo non è legato a un mondoId). CREARE nuovi valori nel catalogo condiviso
     * resta invece riservato agli admin.
     */
    private void assertAdminOrAnyPermesso(Utente utente) {
        if (!authzService.hasAnyPermessoMondo(utente))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Riservato agli admin o a chi ha un permesso su un mondo");
    }

    /**
     * Riepilogo permessi (mondo), SENZA alcun bypass admin: usato dal menu per decidere quali voci
     * mostrare a un utente che non è (o non è in modalità) admin — vedi UpperBar.vue, che combina
     * questo con isRealAdmin && adminMode per l'OR finale. Nessun permesso -> tutti false, anche
     * per un vero admin che qui non ha bypass (l'admin le vede comunque, ma per via del proprio
     * ruolo, non di questa chiamata).
     * <p>
     * "master" resta SEMPRE "almeno un mondo qualsiasi" (Gestione Utenti non è un'azione legata a
     * un mondo specifico). "stats"/"pagine"/"masterMondo" invece, se {@code mondoId} è passato (il
     * mondo "corrente" scelto nel menu), riguardano SOLO quel mondo: un master di Costa che sta
     * guardando Cico non deve vedere Gestione Statistiche/Permessi per mondo/Crea party se non ha
     * quel permesso specifico su Cico, anche se lo ha su Costa. Senza mondoId (mondo corrente non
     * ancora risolto), fallback su "almeno un mondo qualsiasi" anche per questi tre.
     */
    @Operation(
            summary = "I miei permessi (mondo), indipendentemente dall'admin mode",
            description = "master: ho MASTER su almeno un mondo (sempre, non scoped, per Gestione Utenti). " +
                    "stats/pagine/masterMondo: sul mondo indicato in mondoId se presente (per Gestione " +
                    "Statistiche/Permessi per mondo/Crea party), altrimenti su almeno un mondo qualsiasi."
    )
    @GetMapping("/miei-permessi")
    public ResponseEntity<Map<String, Boolean>> getMieiPermessi(
            @RequestParam(required = false) Integer mondoId,
            @AuthenticationPrincipal Utente utente) {
        if (utente == null)
            return ResponseEntity.ok(Map.of("master", false, "stats", false, "pagine", false, "masterMondo", false));
        List<PermessiMondo> mie = permessiMondoRepository.findAllByIdUtente_Id(utente.getId());
        boolean masterOvunque = mie.stream().anyMatch(p -> TipoPermessoMondo.MASTER.equals(p.getPermesso()));
        Map<String, Boolean> result = new LinkedHashMap<>();
        result.put("master", masterOvunque);
        if (mondoId != null) {
            result.put("stats", permessiMondoRepository.existsByIdUtente_IdAndIdMondo_IdAndPermesso(
                    utente.getId(), mondoId, TipoPermessoMondo.STATS));
            result.put("pagine", permessiMondoRepository.existsByIdUtente_IdAndIdMondo_IdAndPermesso(
                    utente.getId(), mondoId, TipoPermessoMondo.PAGINE));
            result.put("masterMondo", permessiMondoRepository.existsByIdUtente_IdAndIdMondo_IdAndPermesso(
                    utente.getId(), mondoId, TipoPermessoMondo.MASTER));
        } else {
            result.put("stats", mie.stream().anyMatch(p -> TipoPermessoMondo.STATS.equals(p.getPermesso())));
            result.put("pagine", mie.stream().anyMatch(p -> TipoPermessoMondo.PAGINE.equals(p.getPermesso())));
            result.put("masterMondo", masterOvunque);
        }
        return ResponseEntity.ok(result);
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

    @Operation(
            summary = "Mondi che l'utente può configurare",
            description = "Tutti i mondi per un admin; solo quelli su cui si ha il permesso PAGINE altrimenti " +
                    "(permessi_mondo) — per il selettore mondo della pagina \"Permessi per mondo\"/Editor per " +
                    "tipo, a differenza di getAll (strettamente admin, usato dalle azioni riservate: crea " +
                    "mondo/sistema, gestione permessi)."
    )
    @GetMapping("/gestibili")
    public ResponseEntity<List<MondoDTO>> getGestibili(@AuthenticationPrincipal Utente utente) {
        List<Mondo> mondi = authzService.isAdmin(utente)
                ? mondoRepository.findAll()
                : permessiMondoRepository.findAllByIdUtente_IdAndPermesso(utente.getId(), TipoPermessoMondo.PAGINE).stream()
                        .map(PermessiMondo::getIdMondo).toList();
        List<MondoDTO> result = mondi.stream().map(this::toDTO)
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

    @Operation(summary = "Permessi su un mondo", description = "Utenti con un permesso (MASTER, STATS o PAGINE) su questo mondo (admin).")
    @GetMapping("/{mondoId}/master")
    public ResponseEntity<List<MasterMondoDTO>> getMaster(@PathVariable Integer mondoId,
                                                          @AuthenticationPrincipal Utente utente) {
        assertAdmin(utente);
        List<MasterMondoDTO> result = permessiMondoRepository.findAllByIdMondo_Id(mondoId).stream()
                .map(pm -> new MasterMondoDTO(pm.getIdUtente().getId(), pm.getIdUtente().getUsername(),
                        pm.getIdUtente().getName(), pm.getPermesso().name()))
                .sorted((a, b) -> {
                    int c = a.getUsername().compareToIgnoreCase(b.getUsername());
                    return c != 0 ? c : a.getPermesso().compareTo(b.getPermesso());
                })
                .toList();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Assegna un permesso (MASTER, STATS o PAGINE) su un mondo a un utente", description = "Admin.")
    @PostMapping("/{mondoId}/master")
    public ResponseEntity<MasterMondoDTO> addMaster(@PathVariable Integer mondoId,
                                                    @Valid @RequestBody AddMasterMondoRequest req,
                                                    @AuthenticationPrincipal Utente utente) {
        assertAdmin(utente);
        Mondo mondo = mondoRepository.findById(mondoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mondo non trovato"));
        Utente target = utenteRepository.findByUsernameIgnoreCase(req.getUsername().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utente non trovato"));

        if (permessiMondoRepository.existsByIdUtente_IdAndIdMondo_IdAndPermesso(target.getId(), mondoId, req.getPermesso()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'utente ha già questo permesso su questo mondo");

        PermessiMondo pm = new PermessiMondo();
        pm.setIdUtente(target);
        pm.setIdMondo(mondo);
        pm.setPermesso(req.getPermesso());
        permessiMondoRepository.save(pm);

        return ResponseEntity.ok(new MasterMondoDTO(target.getId(), target.getUsername(), target.getName(), req.getPermesso().name()));
    }

    @Operation(summary = "Revoca un permesso (MASTER, STATS o PAGINE) su un mondo a un utente", description = "Admin.")
    @DeleteMapping("/{mondoId}/master/{utenteId}")
    @Transactional
    public ResponseEntity<Void> removeMaster(@PathVariable Integer mondoId, @PathVariable Integer utenteId,
                                             @RequestParam TipoPermessoMondo permesso,
                                             @AuthenticationPrincipal Utente utente) {
        assertAdmin(utente);
        permessiMondoRepository.deleteByIdUtente_IdAndIdMondo_IdAndPermesso(utenteId, mondoId, permesso);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Configurazione di un mondo (tipi item e liste/domini incantesimi abilitati)",
            description = "Usata dal frontend per filtrare i menu tipo-item e la multiselect liste/domini " +
                    "nell'editor classe. Aperta a qualunque utente autenticato (non solo admin): serve solo a " +
                    "sapere cosa mostrare, non è un'informazione riservata."
    )
    @GetMapping("/{mondoId}/config")
    public ResponseEntity<MondoConfigDTO> getConfig(@PathVariable Integer mondoId) {
        Mondo mondo = mondoRepository.findById(mondoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mondo non trovato"));

        List<TipoItem> tipi = mondoTipoItemAbilitatoRepository.findAllByMondo_Id(mondoId).stream()
                .map(MondoTipoItemAbilitato::getTipo)
                .sorted()
                .toList();
        List<MondoConfigDTO.ListaIncantesimiDTO> liste = mondoListaIncantesimiAbilitataRepository.findAllByMondo_Id(mondoId).stream()
                .map(MondoListaIncantesimiAbilitata::getListaIncantesimi)
                .map(l -> new MondoConfigDTO.ListaIncantesimiDTO(l.getCodice(), l.getEtichetta()))
                .sorted((a, b) -> a.etichetta().compareToIgnoreCase(b.etichetta()))
                .toList();

        return ResponseEntity.ok(new MondoConfigDTO(tipi, liste, Boolean.TRUE.equals(mondo.getMostraSimboliAzioni()),
                mondo.getSistemaIncantesimi(), mondo.getFormulaManaIncantesimi(), mondo.getFormulaCdIncantesimi(),
                mondo.getListaIncantesimi(), Boolean.TRUE.equals(mondo.getMostraCasterLevel())));
    }

    @Operation(
            summary = "Catalogo globale delle liste/domini incantesimi",
            description = "Tutti i codici esistenti (indipendentemente da cosa è abilitato per un mondo), " +
                    "usato dalla UI di amministrazione per scegliere cosa abilitare in un mondo."
    )
    @GetMapping("/liste-incantesimi")
    public ResponseEntity<List<MondoConfigDTO.ListaIncantesimiDTO>> getCatalogoListeIncantesimi(@AuthenticationPrincipal Utente utente) {
        assertAdminOrAnyPermesso(utente);
        List<MondoConfigDTO.ListaIncantesimiDTO> result = listaIncantesimiRepository.findAll().stream()
                .map(l -> new MondoConfigDTO.ListaIncantesimiDTO(l.getCodice(), l.getEtichetta()))
                .sorted((a, b) -> a.etichetta().compareToIgnoreCase(b.etichetta()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Crea un nuovo codice nel catalogo globale delle liste/domini incantesimi",
            description = "Admin. Il codice è univoco nel catalogo condiviso tra tutti i mondi; non viene " +
                    "abilitato automaticamente da nessuna parte — va poi abilitato per i mondi che lo usano."
    )
    @PostMapping("/liste-incantesimi")
    public ResponseEntity<MondoConfigDTO.ListaIncantesimiDTO> creaListaIncantesimi(
            @Valid @RequestBody CreateListaIncantesimiRequest req, @AuthenticationPrincipal Utente utente) {
        assertAdmin(utente);
        String codice = req.codice().trim().toUpperCase();
        if (codice.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Codice obbligatorio");
        if (listaIncantesimiRepository.existsById(codice)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Codice già esistente nel catalogo");
        }
        ListaIncantesimi lista = new ListaIncantesimi();
        lista.setCodice(codice);
        lista.setEtichetta(req.etichetta().trim());
        listaIncantesimiRepository.save(lista);
        return ResponseEntity.ok(new MondoConfigDTO.ListaIncantesimiDTO(lista.getCodice(), lista.getEtichetta()));
    }

    @Operation(
            summary = "Aggiorna la configurazione abilitata di un mondo",
            description = "Master di quel mondo, o admin. Sostituzione integrale (non incrementale) delle " +
                    "liste passate: un campo null lascia invariata quella parte, una lista vuota disabilita tutto."
    )
    @PutMapping("/{mondoId}/config")
    public ResponseEntity<MondoConfigDTO> aggiornaConfig(@PathVariable Integer mondoId,
                                                         @RequestBody UpdateMondoConfigRequest req,
                                                         @AuthenticationPrincipal Utente utente) {
        assertPagineMondo(utente, mondoId);
        Mondo mondo = mondoRepository.findById(mondoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mondo non trovato"));

        if (req.tipiAbilitati() != null) {
            mondoTipoItemAbilitatoRepository.deleteAll(mondoTipoItemAbilitatoRepository.findAllByMondo_Id(mondoId));
            for (TipoItem tipo : req.tipiAbilitati()) {
                MondoTipoItemAbilitato riga = new MondoTipoItemAbilitato();
                riga.setMondo(mondo);
                riga.setTipo(tipo);
                mondoTipoItemAbilitatoRepository.save(riga);
            }
        }

        if (req.codiciListeIncantesimi() != null) {
            mondoListaIncantesimiAbilitataRepository.deleteAll(mondoListaIncantesimiAbilitataRepository.findAllByMondo_Id(mondoId));
            for (String codice : req.codiciListeIncantesimi()) {
                ListaIncantesimi lista = listaIncantesimiRepository.findById(codice)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lista incantesimi sconosciuta: " + codice));
                MondoListaIncantesimiAbilitata riga = new MondoListaIncantesimiAbilitata();
                riga.setMondo(mondo);
                riga.setListaIncantesimi(lista);
                mondoListaIncantesimiAbilitataRepository.save(riga);
            }
        }

        boolean toccaMondo = false;
        if (req.mostraSimboliAzioni() != null) {
            mondo.setMostraSimboliAzioni(req.mostraSimboliAzioni());
            toccaMondo = true;
        }
        if (req.sistemaIncantesimi() != null) {
            String v = req.sistemaIncantesimi().trim().toUpperCase();
            if (!Constants.SISTEMA_INCANTESIMI_SLOT.equals(v) && !Constants.SISTEMA_INCANTESIMI_MANA.equals(v)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sistemaIncantesimi non valido: " + v);
            }
            mondo.setSistemaIncantesimi(v);
            toccaMondo = true;
        }
        if (req.formulaManaIncantesimi() != null) {
            mondo.setFormulaManaIncantesimi(req.formulaManaIncantesimi().isBlank() ? null : req.formulaManaIncantesimi().trim());
            toccaMondo = true;
        }
        if (req.formulaCdIncantesimi() != null) {
            mondo.setFormulaCdIncantesimi(req.formulaCdIncantesimi().isBlank() ? null : req.formulaCdIncantesimi().trim());
            toccaMondo = true;
        }
        if (req.listaIncantesimi() != null) {
            String v = req.listaIncantesimi().trim().toUpperCase();
            if (!Constants.LISTA_INCANTESIMI_SINGOLA.equals(v) && !Constants.LISTA_INCANTESIMI_MULTIPLA.equals(v)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "listaIncantesimi non valido: " + v);
            }
            mondo.setListaIncantesimi(v);
            toccaMondo = true;
        }
        if (req.mostraCasterLevel() != null) {
            mondo.setMostraCasterLevel(req.mostraCasterLevel());
            toccaMondo = true;
        }
        if (toccaMondo) {
            mondoRepository.save(mondo);
        }

        return getConfig(mondoId);
    }

    private List<TipoItemConfigDTO.OpzioneDTO> parseOpzioni(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<TipoItemConfigDTO.OpzioneDTO>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String writeOpzioni(List<TipoItemConfigDTO.OpzioneDTO> opzioni) {
        if (opzioni == null || opzioni.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(opzioni);
        } catch (Exception e) {
            return null;
        }
    }

    @Operation(
            summary = "Configurazione di un tipo item per un mondo (card strutturali + campi liberi)",
            description = "Aperta a qualunque utente autenticato, come /config: serve solo a sapere cosa " +
                    "mostrare nell'editor. Copre tutti i 36 tipi item, inclusi CLASSE/RAZZA/INCANTESIMO/LIVELLO " +
                    "(ClasseEditor/SpellEditor/LivelloEditor)."
    )
    @GetMapping("/{mondoId}/tipo-item/{tipo}/config")
    public ResponseEntity<TipoItemConfigDTO> getTipoItemConfig(@PathVariable Integer mondoId, @PathVariable TipoItem tipo) {
        if (!mondoRepository.existsById(mondoId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mondo non trovato");

        List<CardEditorItem> card = mondoTipoItemCardAbilitataRepository.findAllByMondo_IdAndTipo(mondoId, tipo).stream()
                .map(MondoTipoItemCardAbilitata::getCard)
                .sorted()
                .toList();
        String titolo = mondoTipoItemMetaRepository.findByMondo_IdAndTipo(mondoId, tipo)
                .map(MondoTipoItemMeta::getCampiTitolo)
                .orElse(null);
        List<TipoItemConfigDTO.CampoLiberoDTO> campi = mondoTipoItemCampoRepository.findAllByMondo_IdAndTipoOrderByOrdineAsc(mondoId, tipo).stream()
                .map(c -> new TipoItemConfigDTO.CampoLiberoDTO(
                        c.getChiave(), c.getEtichetta(),
                        c.getTipoCampo() != null ? c.getTipoCampo().name() : null,
                        c.getPlaceholder(), c.isTextarea(), c.isMultiValore(), c.isHtml(),
                        parseOpzioni(c.getOpzioni())
                ))
                .toList();

        // Scuole/Sottoscuole/Descrittori/Componenti: solo per INCANTESIMO (SpellEditor.vue), liste
        // vuote per ogni altro tipo — non hanno senso altrove.
        boolean isIncantesimo = TipoItem.INCANTESIMO.equals(tipo);
        List<String> scuole = isIncantesimo ? abilitatiCatalogo(mondoId, TipoCatalogoIncantesimo.SCUOLA) : List.of();
        List<String> sottoscuole = isIncantesimo ? abilitatiCatalogo(mondoId, TipoCatalogoIncantesimo.SOTTOSCUOLA) : List.of();
        List<String> descrittori = isIncantesimo ? abilitatiCatalogo(mondoId, TipoCatalogoIncantesimo.DESCRITTORE) : List.of();
        List<String> componenti = isIncantesimo ? abilitatiCatalogo(mondoId, TipoCatalogoIncantesimo.COMPONENTE) : List.of();

        return ResponseEntity.ok(new TipoItemConfigDTO(card, titolo, campi, scuole, sottoscuole, descrittori, componenti));
    }

    private List<String> abilitatiCatalogo(Integer mondoId, TipoCatalogoIncantesimo tipoCatalogo) {
        return mondoCatalogoIncantesimoAbilitatoRepository.findAllByMondo_IdAndTipo(mondoId, tipoCatalogo).stream()
                .map(MondoCatalogoIncantesimoAbilitato::getValore)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private void salvaAbilitatiCatalogo(Mondo mondo, TipoCatalogoIncantesimo tipoCatalogo, List<String> valori) {
        mondoCatalogoIncantesimoAbilitatoRepository.deleteAll(
                mondoCatalogoIncantesimoAbilitatoRepository.findAllByMondo_IdAndTipo(mondo.getId(), tipoCatalogo));
        for (String v : valori) {
            MondoCatalogoIncantesimoAbilitato riga = new MondoCatalogoIncantesimoAbilitato();
            riga.setMondo(mondo);
            riga.setTipo(tipoCatalogo);
            riga.setValore(v);
            mondoCatalogoIncantesimoAbilitatoRepository.save(riga);
        }
    }

    @Operation(
            summary = "Aggiorna la configurazione di un tipo item per un mondo",
            description = "Master di quel mondo, o admin. Sostituzione integrale: un campo null nella request " +
                    "lascia invariata quella parte, una lista vuota disabilita/svuota tutto quel pezzo."
    )
    @PutMapping("/{mondoId}/tipo-item/{tipo}/config")
    public ResponseEntity<TipoItemConfigDTO> aggiornaTipoItemConfig(@PathVariable Integer mondoId, @PathVariable TipoItem tipo,
                                                                    @RequestBody UpdateTipoItemConfigRequest req,
                                                                    @AuthenticationPrincipal Utente utente) {
        assertPagineMondo(utente, mondoId);
        Mondo mondo = mondoRepository.findById(mondoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mondo non trovato"));

        if (req.cardAbilitate() != null) {
            mondoTipoItemCardAbilitataRepository.deleteAll(
                    mondoTipoItemCardAbilitataRepository.findAllByMondo_IdAndTipo(mondoId, tipo));
            for (CardEditorItem card : req.cardAbilitate()) {
                MondoTipoItemCardAbilitata riga = new MondoTipoItemCardAbilitata();
                riga.setMondo(mondo);
                riga.setTipo(tipo);
                riga.setCard(card);
                mondoTipoItemCardAbilitataRepository.save(riga);
            }
        }

        if (req.campiTitolo() != null) {
            MondoTipoItemMeta meta = mondoTipoItemMetaRepository.findByMondo_IdAndTipo(mondoId, tipo).orElseGet(() -> {
                MondoTipoItemMeta nuovo = new MondoTipoItemMeta();
                nuovo.setMondo(mondo);
                nuovo.setTipo(tipo);
                return nuovo;
            });
            meta.setCampiTitolo(req.campiTitolo().isBlank() ? null : req.campiTitolo());
            mondoTipoItemMetaRepository.save(meta);
        }

        if (req.campiLiberi() != null) {
            mondoTipoItemCampoRepository.deleteAll(
                    mondoTipoItemCampoRepository.findAllByMondo_IdAndTipoOrderByOrdineAsc(mondoId, tipo));
            int ordine = 0;
            for (TipoItemConfigDTO.CampoLiberoDTO c : req.campiLiberi()) {
                MondoTipoItemCampo riga = new MondoTipoItemCampo();
                riga.setMondo(mondo);
                riga.setTipo(tipo);
                riga.setChiave(c.chiave());
                riga.setEtichetta(c.etichetta());
                riga.setTipoCampo(c.tipoCampo() != null ? TipoCampoEditor.valueOf(c.tipoCampo()) : null);
                riga.setPlaceholder(c.placeholder());
                riga.setTextarea(c.textarea());
                riga.setMultiValore(c.multiValore());
                riga.setHtml(c.html());
                riga.setOpzioni(writeOpzioni(c.opzioni()));
                riga.setOrdine(ordine++);
                mondoTipoItemCampoRepository.save(riga);
            }
        }

        // Scuole/Sottoscuole/Descrittori/Componenti: solo per INCANTESIMO, ignorati altrimenti
        // anche se valorizzati nella request (nessun senso applicarli ad altri tipi).
        if (TipoItem.INCANTESIMO.equals(tipo)) {
            if (req.scuoleAbilitate() != null) salvaAbilitatiCatalogo(mondo, TipoCatalogoIncantesimo.SCUOLA, req.scuoleAbilitate());
            if (req.sottoscuoleAbilitate() != null) salvaAbilitatiCatalogo(mondo, TipoCatalogoIncantesimo.SOTTOSCUOLA, req.sottoscuoleAbilitate());
            if (req.descrittoriAbilitati() != null) salvaAbilitatiCatalogo(mondo, TipoCatalogoIncantesimo.DESCRITTORE, req.descrittoriAbilitati());
            if (req.componentiAbilitati() != null) salvaAbilitatiCatalogo(mondo, TipoCatalogoIncantesimo.COMPONENTE, req.componentiAbilitati());
        }

        return getTipoItemConfig(mondoId, tipo);
    }

    @Operation(
            summary = "Catalogo globale di una lista di corredo incantesimo (Scuola/Sottoscuola/Descrittore/Componente)",
            description = "Admin. Tutti i valori esistenti (indipendentemente da cosa è abilitato per un mondo), " +
                    "usato dalla UI di amministrazione per scegliere cosa abilitare in un mondo."
    )
    @GetMapping("/catalogo-incantesimo/{tipo}")
    public ResponseEntity<List<String>> getCatalogoIncantesimo(@PathVariable TipoCatalogoIncantesimo tipo,
                                                               @AuthenticationPrincipal Utente utente) {
        assertAdminOrAnyPermesso(utente);
        List<String> result = catalogoIncantesimoRepository.findAllByTipoOrderByValoreAsc(tipo).stream()
                .map(CatalogoIncantesimo::getValore)
                .toList();
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Crea un nuovo valore nel catalogo globale di una lista di corredo incantesimo",
            description = "Admin. Il valore è univoco nel catalogo condiviso tra tutti i mondi (per quel tipo di " +
                    "lista); non viene abilitato automaticamente da nessuna parte."
    )
    @PostMapping("/catalogo-incantesimo/{tipo}")
    public ResponseEntity<String> creaValoreCatalogoIncantesimo(@PathVariable TipoCatalogoIncantesimo tipo,
                                                                @Valid @RequestBody CreaValoreCatalogoIncantesimoRequest req,
                                                                @AuthenticationPrincipal Utente utente) {
        assertAdmin(utente);
        String valore = req.valore().trim();
        if (valore.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valore obbligatorio");
        if (catalogoIncantesimoRepository.existsByTipoAndValore(tipo, valore)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Valore già esistente nel catalogo");
        }
        CatalogoIncantesimo c = new CatalogoIncantesimo();
        c.setTipo(tipo);
        c.setValore(valore);
        catalogoIncantesimoRepository.save(c);
        return ResponseEntity.ok(valore);
    }
}
