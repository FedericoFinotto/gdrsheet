package it.fin8.gdrsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.fin8.gdrsheet.dto.ComentoDTO;
import it.fin8.gdrsheet.dto.SegnalazioneDTO;
import it.fin8.gdrsheet.entity.Utente;
import it.fin8.gdrsheet.service.TaigaClient;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/segnalazioni")
public class SegnalazioniController {

    private final TaigaClient taigaClient;

    public SegnalazioniController(TaigaClient taigaClient) {
        this.taigaClient = taigaClient;
    }

    @Operation(summary = "Segnalazioni: dell'utente corrente, o tutte se all=true. Gli archiviati non sono mai inclusi.")
    @GetMapping
    public ResponseEntity<List<SegnalazioneDTO>> getSegnalazioni(@RequestParam(required = false, defaultValue = "false") boolean all,
                                                                   @AuthenticationPrincipal Utente utente) throws IOException, InterruptedException {
        List<TaigaClient.Segnalazione> segnalazioni = all
                ? taigaClient.listAllUserStories()
                : taigaClient.listUserStoriesByTag(utente.getUsername());
        List<SegnalazioneDTO> out = segnalazioni.stream()
                .map(s -> new SegnalazioneDTO(s.id(), s.ref(), s.titolo(), s.descrizione(), s.stato(), s.dataCreazione(), s.dataModifica()))
                .toList();
        return ResponseEntity.ok(out);
    }

    @Operation(summary = "Dettaglio di una segnalazione (include la descrizione, omessa dalla lista)")
    @GetMapping("/{id}")
    public ResponseEntity<SegnalazioneDTO> getDettaglio(@PathVariable Integer id) throws IOException, InterruptedException {
        TaigaClient.Segnalazione s = taigaClient.getUserStory(id);
        return ResponseEntity.ok(new SegnalazioneDTO(s.id(), s.ref(), s.titolo(), s.descrizione(), s.stato(), s.dataCreazione(), s.dataModifica()));
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
        return ResponseEntity.ok(new SegnalazioneDTO(creata.id(), creata.ref(), creata.titolo(), creata.descrizione(), creata.stato(), creata.dataCreazione(), creata.dataModifica()));
    }

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
