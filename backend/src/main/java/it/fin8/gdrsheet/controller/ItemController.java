package it.fin8.gdrsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.dto.ClasseDetailDTO;
import it.fin8.gdrsheet.dto.ItemDTO;
import it.fin8.gdrsheet.dto.MondoDTO;
import it.fin8.gdrsheet.dto.PageDTO;
import it.fin8.gdrsheet.dto.SpellBookIncantesimoDTO;
import it.fin8.gdrsheet.dto.UpdateItemRequest;
import it.fin8.gdrsheet.dto.UpdateLivelloRequest;
import it.fin8.gdrsheet.dto.UpdateRanghiBulkRequest;
import it.fin8.gdrsheet.dto.UpdatePreparedRequest;
import it.fin8.gdrsheet.dto.UpdateSpellRequest;
import it.fin8.gdrsheet.dto.UpdateSpellUsageRequest;
import it.fin8.gdrsheet.entity.Item;
import it.fin8.gdrsheet.entity.Utente;
import it.fin8.gdrsheet.mapper.ItemMapper;
import it.fin8.gdrsheet.repository.ItemRepository;
import it.fin8.gdrsheet.repository.MondoRepository;
import it.fin8.gdrsheet.repository.SistemaRepository;
import it.fin8.gdrsheet.service.AuthzService;
import it.fin8.gdrsheet.service.ClasseService;
import it.fin8.gdrsheet.service.ItemService;
import it.fin8.gdrsheet.service.PartyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/item")
public class ItemController {

    private final ItemRepository repo;
    private final ItemService itemService;
    private final ItemMapper itemMapper;
    private final AuthzService authzService;
    private final ClasseService classeService;
    private final MondoRepository mondoRepository;
    private final SistemaRepository sistemaRepository;
    private final PartyService partyService;

    public ItemController(ItemRepository repo, ItemService itemService, ItemMapper itemMapper, AuthzService authzService, ClasseService classeService, MondoRepository mondoRepository, SistemaRepository sistemaRepository, PartyService partyService) {
        this.repo = repo;
        this.itemService = itemService;
        this.itemMapper = itemMapper;
        this.authzService = authzService;
        this.classeService = classeService;
        this.mondoRepository = mondoRepository;
        this.sistemaRepository = sistemaRepository;
        this.partyService = partyService;
    }

    @Operation(
            summary = "Recupera un item Specifico",
            description = "Recupera un item Specifico"
    )
    @GetMapping("/{id}")
    public ResponseEntity<Item> getById(
            @Parameter(description = "Id Item", required = true)
            @PathVariable Integer id
    ) {
        Item itm = repo.findItemById(id);

        return ResponseEntity.ok(itm);
    }

    @Operation(
            summary = "Stato abilitato/disabilitato di un item",
            description = "true se l'item è disabilitato nel contesto del personaggio"
    )
    @GetMapping("/{id}/disabled")
    public ResponseEntity<Boolean> isItemDisabled(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer personaggio
    ) {
        return ResponseEntity.ok(itemService.isItemDisabled(id, personaggio));
    }

    @Operation(
            summary = "Recupera i padri di un item",
            description = "Item che hanno questo item come child, escluso il FromCompendio"
    )
    @GetMapping("/{id}/parents")
    public ResponseEntity<List<ItemDTO>> getParents(
            @Parameter(description = "Id Item", required = true)
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(itemService.getParents(id));
    }

    @Operation(
            summary = "Recupera una lista di incantesimi per classe e livello",
            description = "Recupera una lista di incantesimi per classe e livello"
    )
    @GetMapping("incantesimi/{idClasse}/{livello}")
    public ResponseEntity<List<SpellBookIncantesimoDTO>> getById(
            @Parameter(description = "Id Classe", required = true)
            @PathVariable Integer idClasse,
            @Parameter(description = "Livello", required = true)
            @PathVariable Integer livello,
            @Parameter(description = "Liste incantesimi (CSV) della sezione; in fallback la SPELL della classe")
            @RequestParam(required = false) String spellList
    ) {
        List<SpellBookIncantesimoDTO> result = itemService.getListIncantesimiByClasseAndLevel(idClasse, livello, spellList);

        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Abilita/Disabilita Item",
            description = "Abilita/Disabilita Item"
    )
    @GetMapping("switch-state/{idPersonaggio}/{id}")
    public ResponseEntity<Item> switchItemState(
            @Parameter(description = "Id Item", required = true)
            @PathVariable Integer id,
            @Parameter(description = "Id Personaggio", required = true)
            @PathVariable Integer idPersonaggio
    ) {
        Item itm = itemService.switchItemState(id, idPersonaggio);

        return ResponseEntity.ok(itm);
    }

    @Operation(
            summary = "Aggiorna i prepared degli incantesimi",
            description = "Aggiorna il numero di prepared per ciascun incantesimo (spellId -> nprepared) di un personaggio, filtrando per classe e livello."
    )
    @PostMapping("incantesimi/prepara")
    public ResponseEntity<Void> updatePrepared(@Valid @RequestBody UpdatePreparedRequest req) {
        // Validazione minima
        if (req == null || req.getIdPersonaggio() == null || req.getPrepared() == null) {
            return ResponseEntity.badRequest().build();
        }

        // Delego alla service (implementa la logica di persistenza lato tuo ItemService)
        itemService.updatePreparedForCharacterAndLevel(req);

        return ResponseEntity.noContent().build(); // 204
    }

    @Operation(
            summary = "Aggiorna i prepared degli incantesimi",
            description = "Aggiorna il numero di prepared per ciascun incantesimo (spellId -> nprepared) di un personaggio, filtrando per classe e livello."
    )
    @PostMapping("incantesimi/update-spellusage")
    public ResponseEntity<Void> updateSpellUsage(@Valid @RequestBody UpdateSpellUsageRequest req) {

        // Delego alla service (implementa la logica di persistenza lato tuo ItemService)
        itemService.updateSpellUsage(req);

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Aggiorna un incantesimo",
            description = "Aggiorna un incantesimo"
    )
    @PostMapping("/editspell/{id}")
    public ResponseEntity<Item> updateSpell(@PathVariable Integer id,
                                            @RequestBody UpdateSpellRequest dto) {
        // service.updateSpell(id, dto);
        return ResponseEntity.ok(itemService.updateSpell(id, dto));
    }

    @Operation(summary = "Lista tutti i mondi")
    @GetMapping("/mondi")
    public ResponseEntity<List<MondoDTO>> getMondi() {
        return ResponseEntity.ok(mondoRepository.findAll().stream()
                .map(m -> new MondoDTO(m.getId(), m.getDescrizione(),
                        m.getSistema() != null ? m.getSistema().getId() : null,
                        m.getSistema() != null ? m.getSistema().getDescrizione() : null))
                .sorted(java.util.Comparator.comparing(MondoDTO::descrizione, String.CASE_INSENSITIVE_ORDER))
                .toList());
    }

    @Operation(summary = "Lista tutti i sistemi")
    @GetMapping("/sistemi")
    public ResponseEntity<List<MondoDTO>> getSistemi() {
        return ResponseEntity.ok(sistemaRepository.findAll().stream()
                .map(s -> new MondoDTO(s.getId(), s.getDescrizione(), null, null))
                .sorted(java.util.Comparator.comparing(MondoDTO::descrizione, String.CASE_INSENSITIVE_ORDER))
                .toList());
    }

    @Operation(
            summary = "Cerca item per nome",
            description = "Ricerca per nome (contains, case-insensitive), opzionalmente filtrata per tipo. Max 20 risultati."
    )
    @GetMapping("/search")
    public ResponseEntity<List<ItemDTO>> searchItems(
            @Parameter(description = "Testo da cercare nel nome", required = true)
            @RequestParam String q,
            @Parameter(description = "Tipo item (opzionale)")
            @RequestParam(required = false) TipoItem tipo
    ) {
        List<ItemDTO> result = itemService.searchItems(q, tipo).stream().map(itemMapper::toDTO).toList();
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Compendio",
            description = "Lista paginata degli item del compendio (non legati a personaggi), con filtri nome/tipo"
    )
    @GetMapping("/compendio")
    public ResponseEntity<PageDTO<ItemDTO>> getCompendio(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) TipoItem tipo,
            @RequestParam(required = false) Integer idMondo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal Utente utente
    ) {
        String nomeQ = nome == null ? "" : nome.trim();
        var pr = org.springframework.data.domain.PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 50)));
        org.springframework.data.domain.Page<it.fin8.gdrsheet.entity.Item> p;
        if (authzService.isMasterOrAdmin(utente)) {
            p = repo.findCompendioAll(nomeQ, tipo, idMondo, pr);
        } else {
            var mieiMondi = partyService.getMieiMondi(utente);
            var mondoIds = mieiMondi.stream().map(MondoDTO::id).filter(java.util.Objects::nonNull).toList();
            var sistemaIds = mieiMondi.stream().map(MondoDTO::sistemaId).filter(java.util.Objects::nonNull).distinct().toList();
            p = repo.findCompendioForUser(nomeQ, tipo, idMondo, mondoIds, sistemaIds, pr);
        }
        return ResponseEntity.ok(new PageDTO<>(
                p.getContent().stream().map(itemMapper::toDTO).toList(),
                p.getNumber(), p.getSize(), p.getTotalElements(), Math.max(1, p.getTotalPages())
        ));
    }

    @Operation(
            summary = "Dettaglio classe per l'editor",
            description = "Classe con label, tabella dei 20 livelli e abilità concesse"
    )
    @GetMapping("/classe/{id}")
    public ResponseEntity<ClasseDetailDTO> getClasse(
            @Parameter(description = "Id item classe", required = true)
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(classeService.getClasse(id));
    }

    @Operation(
            summary = "Crea/aggiorna una classe completa",
            description = "Salva item CLASSE, item AVANZAMENTO per livello (BAB/TS/DV/GRADI/SP_SLOT) e abilità concesse"
    )
    @PostMapping("/classe")
    public ResponseEntity<ClasseDetailDTO> saveClasse(@Valid @RequestBody ClasseDetailDTO dto) {
        Item saved = classeService.saveClasse(dto);
        return ResponseEntity.ok(classeService.getClasse(saved.getId()));
    }

    @Operation(
            summary = "Crea un nuovo item",
            description = "Crea un nuovo item generico con labels e modificatori"
    )
    @PostMapping("/create")
    public ResponseEntity<Item> createItem(@Valid @RequestBody UpdateItemRequest dto,
                                           @AuthenticationPrincipal Utente utente) {
        if (dto.getIdPersonaggio() != null)
            authzService.assertCanEditPersonaggio(utente, dto.getIdPersonaggio());
        return ResponseEntity.ok(itemService.createItem(dto));
    }

    @Operation(
            summary = "Aggiorna un item generico",
            description = "Aggiorna nome, descrizione, labels e modificatori di un item"
    )
    @PostMapping("/edit/{id}")
    public ResponseEntity<Item> updateItem(@PathVariable Integer id,
                                           @Valid @RequestBody UpdateItemRequest dto,
                                           @RequestParam(required = false) Integer idPersonaggio,
                                           @AuthenticationPrincipal Utente utente) {
        if (idPersonaggio != null)
            authzService.assertCanEditPersonaggio(utente, idPersonaggio);
        return ResponseEntity.ok(itemService.updateItem(id, dto));
    }

    @Operation(
            summary = "Elimina un item",
            description = "Item intestati a un personaggio: eliminazione completa. Item di compendio con idPersonaggio: scollegamento dal FromCompendio (ed eliminazione se non più referenziato)."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(
            @Parameter(description = "Id Item", required = true)
            @PathVariable Integer id,
            @Parameter(description = "Id Personaggio (contesto di scollegamento)")
            @RequestParam(required = false) Integer idPersonaggio,
            @AuthenticationPrincipal Utente utente
    ) {
        if (!authzService.isMasterOrAdmin(utente))
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Solo master e admin possono eliminare gli item");
        if (idPersonaggio != null)
            authzService.assertCanEditPersonaggio(utente, idPersonaggio);
        itemService.deleteItem(id, idPersonaggio);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Aggiorna gli hp consumati di una barriera",
            description = "Imposta BARR_CONS (clampato 0..BARR_MAX) sul talento barriera"
    )
    @PostMapping("/barriera/{id}")
    public ResponseEntity<Void> updateBarriera(
            @Parameter(description = "Id item barriera", required = true)
            @PathVariable Integer id,
            @RequestParam int consumato,
            @RequestParam(required = false) Integer idPersonaggio,
            @AuthenticationPrincipal Utente utente
    ) {
        if (idPersonaggio != null)
            authzService.assertCanEditPersonaggio(utente, idPersonaggio);
        itemService.updateBarriera(id, consumato);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Scollega un item dal personaggio",
            description = "Rimuove l'item dall'equipaggiamento del personaggio (collegamento FromCompendio); l'item resta nel compendio"
    )
    @PostMapping("/unlink/{id}")
    public ResponseEntity<Void> unlinkItem(
            @Parameter(description = "Id Item", required = true)
            @PathVariable Integer id,
            @Parameter(description = "Id Personaggio", required = true)
            @RequestParam Integer idPersonaggio,
            @AuthenticationPrincipal Utente utente
    ) {
        authzService.assertCanEditPersonaggio(utente, idPersonaggio);
        itemService.unlinkItem(id, idPersonaggio);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Collega un item esistente al personaggio",
            description = "Aggiunge un item del compendio all'equipaggiamento del personaggio tramite FromCompendio"
    )
    @PostMapping("/link/{id}")
    public ResponseEntity<Void> linkItem(
            @Parameter(description = "Id Item", required = true) @PathVariable Integer id,
            @Parameter(description = "Id Personaggio", required = true) @RequestParam Integer idPersonaggio,
            @AuthenticationPrincipal Utente utente
    ) {
        authzService.assertCanEditPersonaggio(utente, idPersonaggio);
        itemService.linkItem(id, idPersonaggio);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Aggiorna un item Livello",
            description = "Aggiorna labels, caratteristiche (BASE), ranghi (RANK) e contenuti concessi di un item LIVELLO"
    )
    @PostMapping("/editlivello/{id}")
    public ResponseEntity<Item> updateLivello(@PathVariable Integer id,
                                              @Valid @RequestBody UpdateLivelloRequest dto,
                                              @AuthenticationPrincipal Utente utente) {
        if (dto.getPersonaggioId() != null)
            authzService.assertCanEditPersonaggio(utente, dto.getPersonaggioId());
        return ResponseEntity.ok(itemService.updateLivello(id, dto));
    }

    @Operation(
            summary = "Aggiorna SOLO i ranghi di un item Livello",
            description = "Sostituisce i modificatori RANK del livello senza toccare labels, caratteristiche o contenuti. Usato dalla pagina Gestisci gradi."
    )
    @PostMapping("/editranghi/{id}")
    public ResponseEntity<Item> updateRanghiLivello(@PathVariable Integer id,
                                                    @Valid @RequestBody UpdateLivelloRequest dto,
                                                    @AuthenticationPrincipal Utente utente) {
        if (dto.getPersonaggioId() != null)
            authzService.assertCanEditPersonaggio(utente, dto.getPersonaggioId());
        return ResponseEntity.ok(itemService.updateRanghiLivello(id, dto));
    }

    @Operation(
            summary = "Aggiorna i ranghi di più livelli in un'unica transazione",
            description = "Sostituisce i modificatori RANK di tutti i livelli indicati in un solo persist. Usato dalla pagina Gestisci gradi."
    )
    @PostMapping("/editranghi-bulk")
    public ResponseEntity<Void> updateRanghiBulk(@Valid @RequestBody UpdateRanghiBulkRequest dto,
                                                 @AuthenticationPrincipal Utente utente) {
        if (dto.getPersonaggioId() != null)
            authzService.assertCanEditPersonaggio(utente, dto.getPersonaggioId());
        itemService.updateRanghiLivelliBulk(dto);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Azzera tutti gli utilizzi di un personaggio")
    @DeleteMapping("/utilizzi/{personaggioId}")
    public ResponseEntity<Void> resetUtilizzi(
            @PathVariable Integer personaggioId,
            @AuthenticationPrincipal Utente utente) {
        authzService.assertCanEditPersonaggio(utente, personaggioId);
        itemService.resetUtilizzi(personaggioId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Aggiorna utilizzi di un item per un personaggio")
    @PutMapping("/{itemId}/utilizzi/{personaggioId}")
    public ResponseEntity<Void> setUtilizzi(
            @PathVariable Integer itemId,
            @PathVariable Integer personaggioId,
            @RequestBody java.util.Map<String, Integer> body,
            @AuthenticationPrincipal Utente utente) {
        authzService.assertCanEditPersonaggio(utente, personaggioId);
        Integer usati = body.get("usati");
        if (usati == null) return ResponseEntity.badRequest().build();
        itemService.setUtilizziUsati(itemId, personaggioId, usati);
        return ResponseEntity.noContent().build();
    }

}
