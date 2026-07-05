package it.fin8.gdrsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import it.fin8.gdrsheet.dto.BancaDTO;
import it.fin8.gdrsheet.dto.BancaDetailDTO;
import it.fin8.gdrsheet.dto.CreatePartyRequest;
import it.fin8.gdrsheet.dto.CreatePersonaggioRequest;
import it.fin8.gdrsheet.dto.GiveItemRequest;
import it.fin8.gdrsheet.dto.GruppoDTO;
import it.fin8.gdrsheet.dto.SaveGruppoRequest;
import it.fin8.gdrsheet.dto.AddMembroRequest;
import it.fin8.gdrsheet.dto.MembroPartyDTO;
import it.fin8.gdrsheet.dto.MondoDTO;
import it.fin8.gdrsheet.dto.PageDTO;
import it.fin8.gdrsheet.dto.PartyDetailDTO;
import it.fin8.gdrsheet.dto.PartyItemDTO;
import it.fin8.gdrsheet.dto.ItemSearchResultDTO;
import it.fin8.gdrsheet.dto.MilestonePersonaggioDTO;
import it.fin8.gdrsheet.dto.ApplyMilestoneRequest;
import it.fin8.gdrsheet.entity.Utente;
import it.fin8.gdrsheet.service.PartyService;
import it.fin8.gdrsheet.service.PersonaggioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/party")
public class PartyController {

    private final PartyService partyService;
    private final PersonaggioService personaggioService;

    public PartyController(PartyService partyService, PersonaggioService personaggioService) {
        this.partyService = partyService;
        this.personaggioService = personaggioService;
    }

    @Operation(
            summary = "Mondi del master",
            description = "I mondi a cui l'utente ha accesso (dai suoi party), per creare nuovi party"
    )
    @GetMapping("/mondi")
    public ResponseEntity<List<MondoDTO>> getMieiMondi(@AuthenticationPrincipal Utente utente) {
        return ResponseEntity.ok(partyService.getMieiMondi(utente));
    }

    @Operation(
            summary = "Crea un party",
            description = "Crea un party nel mondo indicato e rende l'utente MASTER"
    )
    @PostMapping
    public ResponseEntity<Integer> createParty(
            @Valid @RequestBody CreatePartyRequest request,
            @AuthenticationPrincipal Utente utente
    ) {
        return ResponseEntity.ok(partyService.createParty(request, utente));
    }

    @Operation(
            summary = "Crea un personaggio",
            description = "Crea un personaggio in un party con il tipo indicato (PG/NPC/BARCA/BANCA/STELLA)"
    )
    @PostMapping("/personaggio")
    public ResponseEntity<Integer> createPersonaggio(
            @Valid @RequestBody CreatePersonaggioRequest request,
            @AuthenticationPrincipal Utente utente
    ) {
        return ResponseEntity.ok(partyService.createPersonaggio(request, utente));
    }

    @Operation(
            summary = "Elimina un party",
            description = "Solo il master del party e solo se non ha personaggi associati"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParty(
            @Parameter(description = "Id Party", required = true)
            @PathVariable Integer id,
            @AuthenticationPrincipal Utente utente
    ) {
        partyService.deleteParty(id, utente);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Membri del party",
            description = "Utenti associati al party con il loro ruolo"
    )
    @GetMapping("/{id}/membri")
    public ResponseEntity<List<MembroPartyDTO>> getMembri(
            @Parameter(description = "Id Party", required = true)
            @PathVariable Integer id,
            @AuthenticationPrincipal Utente utente
    ) {
        return ResponseEntity.ok(partyService.getMembri(id, utente));
    }

    @Operation(
            summary = "Associa un utente al party",
            description = "Solo il master. Ruolo MASTER o GIOCATORE"
    )
    @PostMapping("/{id}/membro")
    public ResponseEntity<MembroPartyDTO> addMembro(
            @Parameter(description = "Id Party", required = true)
            @PathVariable Integer id,
            @Valid @RequestBody AddMembroRequest request,
            @AuthenticationPrincipal Utente utente
    ) {
        return ResponseEntity.ok(partyService.addMembro(id, request, utente));
    }

    @Operation(
            summary = "Dettaglio party",
            description = "Membri del party con i loro soldi (somma dei modificatori MR/MA/MO/MP) e somma complessiva. Accessibile solo ai membri del party."
    )
    @GetMapping("/{id}")
    public ResponseEntity<PartyDetailDTO> getParty(
            @Parameter(description = "Id Party", required = true)
            @PathVariable Integer id,
            @AuthenticationPrincipal Utente utente
    ) {
        return ResponseEntity.ok(partyService.getPartyDetail(id, utente));
    }

    @Operation(
            summary = "Item del party",
            description = "Item di inventario dei membri del party, filtrati per nome/tipo e paginati"
    )
    @GetMapping("/{id}/items")
    public ResponseEntity<PageDTO<PartyItemDTO>> getPartyItems(
            @Parameter(description = "Id Party", required = true)
            @PathVariable Integer id,
            @Parameter(description = "Filtro nome (contains, case-insensitive)")
            @RequestParam(required = false) String nome,
            @Parameter(description = "Filtro tipo item")
            @RequestParam(required = false) String tipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal Utente utente
    ) {
        return ResponseEntity.ok(partyService.getPartyItems(id, utente, nome, tipo, page, size));
    }

    @Operation(
            summary = "Banche del party",
            description = "Personaggi TIPO_PERSONAGGIO=BANCA del party con i loro conti correnti (item con label CC)"
    )
    @GetMapping("/{id}/banche")
    public ResponseEntity<List<BancaDTO>> getBanche(
            @Parameter(description = "Id Party", required = true)
            @PathVariable Integer id,
            @AuthenticationPrincipal Utente utente
    ) {
        return ResponseEntity.ok(partyService.getBanche(id, utente));
    }

    @Operation(
            summary = "Dettaglio banca",
            description = "Conti correnti della banca raggruppati per party. Vista completa per i membri del party della banca, filtrata per gli altri."
    )
    @GetMapping("/banca/{bancaId}/dettaglio")
    public ResponseEntity<BancaDetailDTO> getBancaDetail(
            @Parameter(description = "Id personaggio banca", required = true)
            @PathVariable Integer bancaId,
            @AuthenticationPrincipal Utente utente
    ) {
        return ResponseEntity.ok(partyService.getBancaDetail(bancaId, utente));
    }

    @Operation(
            summary = "Apre un conto in banca",
            description = "Crea l'item conto (label CC = G<idPersonaggio> | P<idParty>) nella banca indicata"
    )
    @PostMapping("/banca/{bancaId}/conto")
    public ResponseEntity<BancaDTO.ContoDTO> apriConto(
            @Parameter(description = "Id personaggio banca", required = true)
            @PathVariable Integer bancaId,
            @Parameter(description = "CC: G<idPersonaggio> oppure P<idParty>", required = true)
            @RequestParam String cc,
            @AuthenticationPrincipal Utente utente
    ) {
        return ResponseEntity.ok(partyService.apriConto(bancaId, cc, utente));
    }

    @Operation(
            summary = "Aggiorna un conto banca",
            description = "Imposta le monete (MR/MA/MO/MP) del conto"
    )
    @PostMapping("/banca/conto/{itemId}")
    public ResponseEntity<PartyDetailDTO.SoldiDTO> updateConto(
            @Parameter(description = "Id item conto", required = true)
            @PathVariable Integer itemId,
            @Valid @RequestBody PartyDetailDTO.SoldiDTO soldi,
            @AuthenticationPrincipal Utente utente
    ) {
        return ResponseEntity.ok(partyService.updateConto(itemId, soldi, utente));
    }

    @Operation(
            summary = "Sposta un item tra inventari",
            description = "Sposta un item dall'inventario di un personaggio a un altro dello stesso party"
    )
    @PostMapping("/give")
    public ResponseEntity<Void> giveItem(
            @Valid @RequestBody GiveItemRequest request,
            @AuthenticationPrincipal Utente utente
    ) {
        partyService.giveItem(request, utente);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Ricerca profonda tra gli item del party",
            description = "Cerca in tutti gli item di tutti i personaggi del party (qualsiasi tipo), " +
                    "sul nome, sul valore delle label e sulle note dei modificatori."
    )
    @GetMapping("/{id}/search-items")
    public ResponseEntity<List<ItemSearchResultDTO>> searchItemsParty(
            @PathVariable Integer id,
            @RequestParam String q,
            @AuthenticationPrincipal Utente utente
    ) {
        partyService.assertMembroParty(id, utente); // verifica appartenenza al party
        return ResponseEntity.ok(personaggioService.searchItemsParty(id, q));
    }

    @Operation(summary = "Gruppi del party", description = "Gruppi con membri e capogruppo. Accessibile ai membri del party.")
    @GetMapping("/{id}/gruppi")
    public ResponseEntity<List<GruppoDTO>> getGruppi(
            @PathVariable Integer id,
            @AuthenticationPrincipal Utente utente
    ) {
        return ResponseEntity.ok(partyService.getGruppi(id, utente));
    }

    @Operation(summary = "Crea un gruppo", description = "Solo il master del party")
    @PostMapping("/{id}/gruppo")
    public ResponseEntity<GruppoDTO> createGruppo(
            @PathVariable Integer id,
            @RequestBody SaveGruppoRequest request,
            @AuthenticationPrincipal Utente utente
    ) {
        return ResponseEntity.ok(partyService.createGruppo(id, request.getNome(), utente));
    }

    @Operation(summary = "Aggiorna un gruppo", description = "Nome, membri e capogruppo (stato completo). Solo il master del party.")
    @PutMapping("/gruppo/{gruppoId}")
    public ResponseEntity<GruppoDTO> saveGruppo(
            @PathVariable Integer gruppoId,
            @RequestBody SaveGruppoRequest request,
            @AuthenticationPrincipal Utente utente
    ) {
        return ResponseEntity.ok(partyService.saveGruppo(gruppoId, request, utente));
    }

    @Operation(summary = "Elimina un gruppo", description = "Rimuove il gruppo e l'appartenenza dei membri. Solo il master del party.")
    @DeleteMapping("/gruppo/{gruppoId}")
    public ResponseEntity<Void> deleteGruppo(
            @PathVariable Integer gruppoId,
            @AuthenticationPrincipal Utente utente
    ) {
        partyService.deleteGruppo(gruppoId, utente);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Personaggi livellabili di un gruppo",
            description = "PG e STELLA del gruppo con milestone attuali, livello e saghe per il prossimo livello.")
    @GetMapping("/gruppo/{gruppoId}/milestone")
    public ResponseEntity<List<MilestonePersonaggioDTO>> getMilestoneGruppo(
            @PathVariable Integer gruppoId,
            @AuthenticationPrincipal Utente utente
    ) {
        return ResponseEntity.ok(partyService.getMilestoneGruppo(gruppoId, utente));
    }

    @Operation(summary = "Livella un gruppo",
            description = "Aggiunge milestone ai personaggi selezionati, applicando i passaggi di livello.")
    @PostMapping("/gruppo/{gruppoId}/milestone")
    public ResponseEntity<List<MilestonePersonaggioDTO>> applyMilestoneGruppo(
            @PathVariable Integer gruppoId,
            @RequestBody ApplyMilestoneRequest request,
            @AuthenticationPrincipal Utente utente
    ) {
        int quantita = request.getQuantita() != null ? request.getQuantita() : 1;
        return ResponseEntity.ok(partyService.applyMilestoneGruppo(gruppoId, request.getPersonaggioIds(), quantita, utente));
    }
}
