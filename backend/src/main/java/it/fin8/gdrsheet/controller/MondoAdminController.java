package it.fin8.gdrsheet.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import it.fin8.gdrsheet.def.CardEditorItem;
import it.fin8.gdrsheet.def.TipoCampoEditor;
import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.def.TipoPermessoMondo;
import it.fin8.gdrsheet.dto.AddMasterMondoRequest;
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
import it.fin8.gdrsheet.entity.ListaIncantesimi;
import it.fin8.gdrsheet.entity.Mondo;
import it.fin8.gdrsheet.entity.MondoListaIncantesimiAbilitata;
import it.fin8.gdrsheet.entity.MondoTipoItemAbilitato;
import it.fin8.gdrsheet.entity.MondoTipoItemCampo;
import it.fin8.gdrsheet.entity.MondoTipoItemCardAbilitata;
import it.fin8.gdrsheet.entity.MondoTipoItemMeta;
import it.fin8.gdrsheet.entity.PermessiMondo;
import it.fin8.gdrsheet.entity.Sistema;
import it.fin8.gdrsheet.entity.Utente;
import it.fin8.gdrsheet.repository.ListaIncantesimiRepository;
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
 * <p>
 * Espone anche la configurazione per-mondo di cosa è abilitato (tipi item, liste/domini
 * incantesimi): vedi {@link MondoConfigDTO}.
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

    @Operation(
            summary = "Configurazione di un mondo (tipi item e liste/domini incantesimi abilitati)",
            description = "Usata dal frontend per filtrare i menu tipo-item e la multiselect liste/domini " +
                    "nell'editor classe. Aperta a qualunque utente autenticato (non solo admin): serve solo a " +
                    "sapere cosa mostrare, non è un'informazione riservata."
    )
    @GetMapping("/{mondoId}/config")
    public ResponseEntity<MondoConfigDTO> getConfig(@PathVariable Integer mondoId) {
        if (!mondoRepository.existsById(mondoId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mondo non trovato");

        List<TipoItem> tipi = mondoTipoItemAbilitatoRepository.findAllByMondo_Id(mondoId).stream()
                .map(MondoTipoItemAbilitato::getTipo)
                .sorted()
                .toList();
        List<MondoConfigDTO.ListaIncantesimiDTO> liste = mondoListaIncantesimiAbilitataRepository.findAllByMondo_Id(mondoId).stream()
                .map(MondoListaIncantesimiAbilitata::getListaIncantesimi)
                .map(l -> new MondoConfigDTO.ListaIncantesimiDTO(l.getCodice(), l.getEtichetta()))
                .sorted((a, b) -> a.etichetta().compareToIgnoreCase(b.etichetta()))
                .toList();

        return ResponseEntity.ok(new MondoConfigDTO(tipi, liste));
    }

    @Operation(
            summary = "Catalogo globale delle liste/domini incantesimi",
            description = "Tutti i codici esistenti (indipendentemente da cosa è abilitato per un mondo), " +
                    "usato dalla UI di amministrazione per scegliere cosa abilitare in un mondo."
    )
    @GetMapping("/liste-incantesimi")
    public ResponseEntity<List<MondoConfigDTO.ListaIncantesimiDTO>> getCatalogoListeIncantesimi(@AuthenticationPrincipal Utente utente) {
        assertAdmin(utente);
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
            description = "Admin. Sostituzione integrale (non incrementale) delle liste passate: un campo " +
                    "null lascia invariata quella parte, una lista vuota disabilita tutto."
    )
    @PutMapping("/{mondoId}/config")
    public ResponseEntity<MondoConfigDTO> aggiornaConfig(@PathVariable Integer mondoId,
                                                         @RequestBody UpdateMondoConfigRequest req,
                                                         @AuthenticationPrincipal Utente utente) {
        assertAdmin(utente);
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

        return ResponseEntity.ok(new TipoItemConfigDTO(card, titolo, campi));
    }

    @Operation(
            summary = "Aggiorna la configurazione di un tipo item per un mondo",
            description = "Admin. Sostituzione integrale: un campo null nella request lascia invariata quella " +
                    "parte, una lista vuota disabilita/svuota tutto quel pezzo."
    )
    @PutMapping("/{mondoId}/tipo-item/{tipo}/config")
    public ResponseEntity<TipoItemConfigDTO> aggiornaTipoItemConfig(@PathVariable Integer mondoId, @PathVariable TipoItem tipo,
                                                                    @RequestBody UpdateTipoItemConfigRequest req,
                                                                    @AuthenticationPrincipal Utente utente) {
        assertAdmin(utente);
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

        return getTipoItemConfig(mondoId, tipo);
    }
}
