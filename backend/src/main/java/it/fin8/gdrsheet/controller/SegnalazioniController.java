package it.fin8.gdrsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.fin8.gdrsheet.dto.AllegatoDTO;
import it.fin8.gdrsheet.dto.ComentoDTO;
import it.fin8.gdrsheet.dto.SegnalazioneDTO;
import it.fin8.gdrsheet.entity.Utente;
import it.fin8.gdrsheet.service.SegnalazioneVisteService;
import it.fin8.gdrsheet.service.TaigaClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/segnalazioni")
public class SegnalazioniController {

    private final TaigaClient taigaClient;
    private final SegnalazioneVisteService visteService;

    public SegnalazioniController(TaigaClient taigaClient, SegnalazioneVisteService visteService) {
        this.taigaClient = taigaClient;
        this.visteService = visteService;
    }

    @Operation(summary = "Mappa (id segnalazione -> timestamp ISO) dell'ultima volta che l'utente corrente ha visto ciascuna")
    @GetMapping("/viste")
    public ResponseEntity<Map<String, String>> getViste(@AuthenticationPrincipal Utente utente) {
        return ResponseEntity.ok(visteService.getViste(utente.getUsername()));
    }

    @Operation(summary = "Segna una segnalazione come vista ora dall'utente corrente")
    @PutMapping("/{id}/vista")
    public ResponseEntity<Void> segnaVista(@PathVariable Integer id, @AuthenticationPrincipal Utente utente) {
        visteService.segnaVista(utente.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Segnalazioni: dell'utente corrente, o tutte se all=true. Gli archiviati sono inclusi solo se archiviate=true.")
    @GetMapping
    public ResponseEntity<List<SegnalazioneDTO>> getSegnalazioni(@RequestParam(required = false, defaultValue = "false") boolean all,
                                                                   @RequestParam(required = false, defaultValue = "false") boolean archiviate,
                                                                   @AuthenticationPrincipal Utente utente) throws IOException, InterruptedException {
        List<TaigaClient.Segnalazione> segnalazioni = all
                ? taigaClient.listAllUserStories(archiviate)
                : taigaClient.listUserStoriesByTag(utente.getUsername(), archiviate);
        List<SegnalazioneDTO> out = segnalazioni.stream()
                .map(s -> toDTO(s, utente))
                .toList();
        return ResponseEntity.ok(out);
    }

    @Operation(summary = "Dettaglio di una segnalazione (include la descrizione, omessa dalla lista)")
    @GetMapping("/{id}")
    public ResponseEntity<SegnalazioneDTO> getDettaglio(@PathVariable Integer id,
                                                         @AuthenticationPrincipal Utente utente) throws IOException, InterruptedException {
        TaigaClient.Segnalazione s = taigaClient.getUserStory(id);
        return ResponseEntity.ok(toDTO(s, utente));
    }

    @Operation(summary = "Crea una nuova segnalazione su Taiga, etichettata con l'utente corrente")
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<SegnalazioneDTO> crea(@RequestParam String titolo,
                                                 @RequestParam(required = false) String descrizione,
                                                 @RequestParam(required = false) MultipartFile screenshot,
                                                 @AuthenticationPrincipal Utente utente) throws IOException, InterruptedException {
        TaigaClient.Segnalazione creata = taigaClient.createUserStory(titolo, descrizione, utente.getUsername());
        if (screenshot != null && !screenshot.isEmpty()) {
            taigaClient.uploadAttachment(creata.id(), screenshot);
        }
        return ResponseEntity.ok(toDTO(creata, utente));
    }

    @Operation(summary = "Modifica titolo/descrizione: solo il proprietario (etichetta = suo username) può farlo")
    @PatchMapping("/{id}")
    public ResponseEntity<SegnalazioneDTO> modifica(@PathVariable Integer id,
                                                      @RequestBody ModificaRequest request,
                                                      @AuthenticationPrincipal Utente utente) throws IOException, InterruptedException {
        TaigaClient.Segnalazione corrente = taigaClient.getUserStory(id);
        if (!corrente.tags().contains(utente.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Puoi modificare solo le tue segnalazioni");
        }
        TaigaClient.Segnalazione aggiornata = taigaClient.updateUserStory(id, request.titolo(), request.descrizione());
        return ResponseEntity.ok(toDTO(aggiornata, utente));
    }

    @Operation(summary = "Allegati di una segnalazione")
    @GetMapping("/{id}/allegati")
    public ResponseEntity<List<AllegatoDTO>> getAllegati(@PathVariable Integer id) throws IOException, InterruptedException {
        List<AllegatoDTO> out = taigaClient.getAttachments(id).stream()
                .map(a -> new AllegatoDTO(a.id(), a.nome(), a.url()))
                .toList();
        return ResponseEntity.ok(out);
    }

    @Operation(summary = "Scarica il contenuto (bytes) di un allegato immagine, autenticandosi lato server verso Taiga")
    @GetMapping("/{id}/allegati/{allegatoId}/contenuto")
    public ResponseEntity<byte[]> getContenutoAllegato(@PathVariable Integer id, @PathVariable Integer allegatoId) throws IOException, InterruptedException {
        TaigaClient.Allegato allegato = taigaClient.getAttachments(id).stream()
                .filter(a -> a.id().equals(allegatoId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Allegato non trovato"));
        TaigaClient.Contenuto contenuto = taigaClient.scaricaAllegato(allegato.url());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contenuto.contentType()))
                .body(contenuto.bytes());
    }

    private static SegnalazioneDTO toDTO(TaigaClient.Segnalazione s, Utente utente) {
        boolean mia = s.tags() != null && s.tags().contains(utente.getUsername());
        return new SegnalazioneDTO(s.id(), s.ref(), s.titolo(), s.descrizione(), s.stato(), s.dataCreazione(), s.dataModifica(), mia);
    }

    public record ModificaRequest(String titolo, String descrizione) {}

    @Operation(summary = "Commenti di una segnalazione")
    @GetMapping("/{id}/commenti")
    public ResponseEntity<List<ComentoDTO>> getCommenti(@PathVariable Integer id) throws IOException, InterruptedException {
        List<ComentoDTO> out = taigaClient.getComments(id).stream()
                .map(c -> new ComentoDTO(c.autore(), c.testo(), c.data()))
                .toList();
        return ResponseEntity.ok(out);
    }

    @Operation(summary = "Aggiunge un commento a una segnalazione")
    @PostMapping("/{id}/commenti")
    public ResponseEntity<ComentoDTO> aggiungiCommento(@PathVariable Integer id,
                                                         @RequestBody ComentoRequest request,
                                                         @AuthenticationPrincipal Utente utente) throws IOException, InterruptedException {
        TaigaClient.Commento commento = taigaClient.addComment(id, request.testo(), utente.getUsername());
        return ResponseEntity.ok(new ComentoDTO(commento.autore(), commento.testo(), commento.data()));
    }

    public record ComentoRequest(String testo) {}
}
