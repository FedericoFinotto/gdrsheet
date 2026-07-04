package it.fin8.gdrsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.dto.*;
import it.fin8.gdrsheet.entity.Personaggio;
import it.fin8.gdrsheet.mapper.ItemMapper;
import it.fin8.gdrsheet.repository.PersonaggioRepository;
import it.fin8.gdrsheet.entity.Utente;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import it.fin8.gdrsheet.service.AuthzService;
import it.fin8.gdrsheet.service.ItemService;
import it.fin8.gdrsheet.service.ModificatoriService;
import it.fin8.gdrsheet.service.PartyService;
import it.fin8.gdrsheet.service.PersonaggioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/personaggi")
public class PersonaggioController {

    private final PersonaggioRepository repo;

    private final PersonaggioService personaggioService;
    private final ItemMapper itemMapper;
    private final ModificatoriService modificatoriService;
    private final PartyService partyService;
    private final ItemService itemService;
    private final AuthzService authzService;

    public PersonaggioController(PersonaggioRepository repo, PersonaggioService personaggioService, ItemMapper itemMapper, ModificatoriService modificatoriService, PartyService partyService, ItemService itemService, AuthzService authzService) {
        this.repo = repo;
        this.personaggioService = personaggioService;
        this.itemMapper = itemMapper;
        this.modificatoriService = modificatoriService;
        this.partyService = partyService;
        this.authzService = authzService;
        this.itemService = itemService;
    }

    @Operation(
            summary = "Recupera un personaggio per ID",
            description = "Restituisce i dati base di un personaggio (nome, razza, classe, livello e riferimenti a utente, mondo, sistema)."
    )
    @GetMapping("/{id}")
    public ResponseEntity<Personaggio> getById(
            @Parameter(description = "ID del personaggio", required = true)
            @PathVariable Integer id
    ) {
        Personaggio p = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Personaggio non trovato"));

        return ResponseEntity.ok(p);
    }

    @Operation(
            summary = "Recupera un personaggio per ID",
            description = "Items legati a un personaggio"
    )
    @GetMapping("/items/{id}")
    public ResponseEntity<ItemsDTO> getAllPersonaggioItemsDTOByIdPersonaggio(
            @Parameter(description = "ID del personaggio", required = true)
            @PathVariable Integer id,
            @AuthenticationPrincipal Utente utente
    ) {
        // garantisce FromCompendio, PreparedSpell e stat_value completi all'apertura del personaggio
        itemService.ensureFromCompendio(id);
        itemService.ensurePreparedSpell(id);
        personaggioService.ensureStatValues(id);

        DatiPersonaggioDTO dati = personaggioService.getDatiPersonaggio(id);
        ItemsDTO result = personaggioService.getAllPersonaggioItemsDTOByIdPersonaggio(id, utente, dati.getUtilizziTotaleFormula());

        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Ricerca profonda tra gli item del personaggio",
            description = "Cerca in qualsiasi tipo di item (privilegi, razza, abilità, talenti, competenze, oggetti…), " +
                    "sul nome, sul valore delle label e sulle note dei modificatori."
    )
    @GetMapping("/{id}/search-items")
    public ResponseEntity<List<ItemSearchResultDTO>> searchItemsPersonaggio(
            @PathVariable Integer id,
            @RequestParam String q
    ) {
        return ResponseEntity.ok(personaggioService.searchItemsPersonaggio(id, q));
    }

    @Operation(
            summary = "Modificatori Personaggio",
            description = "Restituisce i dati base di un personaggio (nome, razza, classe, livello e riferimenti a utente, mondo, sistema)."
    )
    @GetMapping("/modificatori/{id}")
    public ResponseEntity<DatiPersonaggioDTO> modificatoriPersonaggio(
            @Parameter(description = "ID del personaggio", required = true)
            @PathVariable Integer id
    ) {
        DatiPersonaggioDTO result = personaggioService.getDatiPersonaggio(id);

        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Aggiorna le info anagrafiche del personaggio",
            description = "Aggiorna nome e personaggio_label (luogo/data nascita, razza, sesso, peso, ecc.)"
    )
    @PostMapping("/{id}/info")
    public ResponseEntity<DatiPersonaggioDTO> updateInfo(
            @Parameter(description = "ID Personaggio", required = true)
            @PathVariable Integer id,
            @Valid @RequestBody UpdateInfoPersonaggioRequest request,
            @AuthenticationPrincipal Utente utente
    ) {
        authzService.assertCanEditPersonaggio(utente, id);
        personaggioService.updateInfoPersonaggio(id, request.getNome(), request.getInfo());
        return ResponseEntity.ok(personaggioService.getDatiPersonaggio(id));
    }

    @Operation(
            summary = "Aggiorna i punti ferita",
            description = "Aggiorna i punti ferita"
    )
    @PostMapping("/update-hp")
    public ResponseEntity<Boolean> updateHp(
            @Parameter(description = "Aggiorna i punti ferita", required = true)
            @Valid @RequestBody UpdateHPRequest request
    ) {
        Boolean resp = personaggioService.updateHP(request);
        return ResponseEntity.ok(resp);
    }

    @Operation(
            summary = "Aggiorna i punti ferita",
            description = "Aggiorna i punti ferita"
    )
    @PostMapping("/update-counter")
    public ResponseEntity<Boolean> updateCounter(
            @Parameter(description = "Aggiorna i punti ferita", required = true)
            @Valid @RequestBody UpdateCounterRequest request
    ) {
        Boolean resp = personaggioService.updateContatore(request);
        return ResponseEntity.ok(resp);
    }

    @Operation(
            summary = "Aggiorna i prepared degli incantesimi",
            description = "Aggiorna il numero di prepared per ciascun incantesimo (spellId -> nprepared) di un personaggio, filtrando per classe e livello."
    )
    @PostMapping("stat/update-base")
    public ResponseEntity<Void> updataBaseStatValue(@Valid @RequestBody UpdateBaseStatValueRequest req) {

        // Delego alla service (implementa la logica di persistenza lato tuo ItemService)
        personaggioService.updateBaseStatValue(req);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("stat/update")
    public ResponseEntity<Void> updateStatValue(@RequestBody UpdateStatValueRequest req) {
        personaggioService.updateStatValue(req);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Soldi del personaggio",
            description = "Totale monete (MR/MA/MO/MP) del personaggio, somma dei modificatori sugli item attivi"
    )
    @GetMapping("/{id}/soldi")
    public ResponseEntity<PartyDetailDTO.SoldiDTO> getSoldi(
            @Parameter(description = "ID Personaggio", required = true)
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(partyService.calcolaSoldi(id));
    }

    @Operation(
            summary = "Conti banca del personaggio",
            description = "I conti correnti intestati al personaggio nelle varie banche"
    )
    @GetMapping("/{id}/conti")
    public ResponseEntity<List<BancaDTO>> getConti(
            @Parameter(description = "ID Personaggio", required = true)
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(partyService.getContiPersonaggio(id));
    }

    @Operation(
            summary = "Aggiorna i soldi del personaggio",
            description = "Imposta i totali monete; la differenza viene scritta sull'item Borsellino (creato se mancante)"
    )
    @PostMapping("/{id}/soldi")
    public ResponseEntity<PartyDetailDTO.SoldiDTO> updateSoldi(
            @Parameter(description = "ID Personaggio", required = true)
            @PathVariable Integer id,
            @Valid @RequestBody PartyDetailDTO.SoldiDTO soldi,
            @AuthenticationPrincipal Utente utente
    ) {
        authzService.assertCanEditPersonaggio(utente, id);
        return ResponseEntity.ok(partyService.updateSoldi(id, soldi));
    }

    @Operation(
            summary = "A partire dall'id di un item Livello restituisce il personaggio associato",
            description = "A partire dall'id di un item Livello restituisce il personaggio associato"
    )
    @GetMapping("/id-personaggio-da-livello/{id}")
    public ResponseEntity<Integer> getPersonaggioByLivello(
            @Parameter(description = "ID livello", required = true)
            @PathVariable Integer id
    ) {
        Integer result = personaggioService.getPersonaggioIdDaLivello(id);

        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Restituisce le statistiche associabili a un personaggio e i suoi valori giá presenti",
            description = "Restituisce le statistiche associabili a un personaggio e i suoi valori giá presenti"
    )
    @GetMapping("/stats/{id}")
    public ResponseEntity<List<AbilitaDTO>> getStatByPersonaggio(
            @Parameter(description = "ID Personaggio", required = true)
            @PathVariable Integer id
    ) {
        List<AbilitaDTO> result = personaggioService.getStatsDaPersonaggio(id);

        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Restituisce le classi e razze associabili a un personaggio",
            description = "Restituisce le classi e razze associabili a un personaggio (gestite con lo stesso editor)"
    )
    @GetMapping("/classi-associabili/{id}")
    public ResponseEntity<List<ItemDTO>> getClassiAssociabiliPersonaggio(
            @Parameter(description = "ID Personaggio", required = true)
            @PathVariable Integer id
    ) {
        List<ItemDTO> result = new ArrayList<>();
        result.addAll(personaggioService.getItemAssociabili(id, TipoItem.CLASSE).stream().map(itemMapper::toDTO).toList());
        result.addAll(personaggioService.getItemAssociabili(id, TipoItem.RAZZA).stream().map(itemMapper::toDTO).toList());

        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Restituisce le classi associabili a un personaggio",
            description = "Restituisce le classi associabili a un personaggio"
    )
    @GetMapping("/maledizioni-associabili/{id}")
    public ResponseEntity<List<ItemDTO>> getMaledizioniAssociabiliPersonaggio(
            @Parameter(description = "ID Personaggio", required = true)
            @PathVariable Integer id
    ) {
        List<ItemDTO> maledizioni = personaggioService.getItemAssociabili(id, TipoItem.MALEDIZIONE).stream().map(itemMapper::toDTO).toList();
        List<ItemDTO> frutti = personaggioService.getItemAssociabili(id, TipoItem.FRUTTO).stream().map(itemMapper::toDTO).toList();
        List<ItemDTO> result = new ArrayList<>();
        result.addAll(maledizioni);
        result.addAll(frutti);

        return ResponseEntity.ok(result);
    }


    @Operation(
            summary = "Restituisce le classi associabili a un personaggio",
            description = "Restituisce le classi associabili a un personaggio"
    )
    @GetMapping("/abilita-classe/{idPersonaggio}/{livello}/{idClasse}")
    public ResponseEntity<List<AbilitaClasseDTO>> getAbilitaClassePerClassePersonaggio(
            @Parameter(description = "ID Personaggio", required = true)
            @PathVariable Integer idPersonaggio,
            @Parameter(description = "Livello", required = true)
            @PathVariable Integer livello,
            @Parameter(description = "ID Classe", required = true)
            @PathVariable Integer idClasse
    ) {
        List<AbilitaClasseDTO> result = modificatoriService.getAbilitaClasse(idPersonaggio, livello, idClasse);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Restituisce le classi associabili a un personaggio",
            description = "Restituisce le classi associabili a un personaggio"
    )
    @GetMapping("/gradi-livello/{idPersonaggio}/{livello}/{livelli}/{idClasse}")
    public ResponseEntity<GradiDTO> getGradiLivelloPerClassePersonaggio(
            @Parameter(description = "ID Personaggio", required = true)
            @PathVariable Integer idPersonaggio,
            @Parameter(description = "Livello", required = true)
            @PathVariable Integer livello,
            @Parameter(description = "ID Classe", required = true)
            @PathVariable Integer idClasse,
            @Parameter(description = "Livelli", required = true)
            @PathVariable String livelli
    ) {
        GradiDTO result = personaggioService.getGradi(idPersonaggio, livello, idClasse, livelli);
        return ResponseEntity.ok(result);
    }


}
