package it.fin8.gdrsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.fin8.gdrsheet.dto.ChangePasswordRequest;
import it.fin8.gdrsheet.dto.CreateUserRequest;
import it.fin8.gdrsheet.dto.LoginResponse;
import it.fin8.gdrsheet.dto.SetUtenteLabelRequest;
import it.fin8.gdrsheet.dto.UpdateProfileRequest;
import it.fin8.gdrsheet.dto.UtenteAdminDTO;
import it.fin8.gdrsheet.entity.Utente;
import it.fin8.gdrsheet.entity.UtenteLabel;
import it.fin8.gdrsheet.repository.UtenteLabelRepository;
import it.fin8.gdrsheet.service.AuthService;
import it.fin8.gdrsheet.service.AuthzService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthService authService;
    private final AuthzService authzService;
    private final UtenteLabelRepository utenteLabelRepository;

    public UserController(AuthService authService, AuthzService authzService, UtenteLabelRepository utenteLabelRepository) {
        this.authService = authService;
        this.authzService = authzService;
        this.utenteLabelRepository = utenteLabelRepository;
    }

    /**
     * Preferenze/stato UI legati all'utente loggato (stessa logica di ItemLabel): una riga per
     * chiave, valore libero. Primo utilizzo: l'ultimo mondo aperto, per lo switcher mondo nel
     * menu — ma pensato per qualunque altra preferenza futura, senza nuovi endpoint dedicati.
     */
    @Operation(summary = "Legge una preferenza dell'utente loggato")
    @GetMapping("/me/label/{label}")
    public ResponseEntity<String> getMyLabel(@PathVariable String label, @AuthenticationPrincipal Utente utente) {
        return utenteLabelRepository.findByUtente_IdAndLabel(utente.getId(), label)
                .map(l -> ResponseEntity.ok(l.getValore()))
                .orElse(ResponseEntity.noContent().build());
    }

    @Operation(summary = "Imposta (crea o aggiorna) una preferenza dell'utente loggato")
    @PutMapping("/me/label/{label}")
    public ResponseEntity<Void> setMyLabel(@PathVariable String label, @RequestBody SetUtenteLabelRequest req,
                                           @AuthenticationPrincipal Utente utente) {
        UtenteLabel l = utenteLabelRepository.findByUtente_IdAndLabel(utente.getId(), label).orElseGet(UtenteLabel::new);
        l.setUtente(utente);
        l.setLabel(label);
        l.setValore(req.getValore());
        utenteLabelRepository.save(l);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Aggiorna profilo dell'utente loggato (username, name)")
    @PutMapping("/me")
    public ResponseEntity<LoginResponse.UtenteDTO> updateProfile(@RequestBody UpdateProfileRequest req,
                                                                 @AuthenticationPrincipal Utente utente) {
        return ResponseEntity.ok(authService.updateProfile(utente, req.getUsername(), req.getName()));
    }

    @Operation(summary = "Cambia/imposta la password dell'utente loggato")
    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req,
                                               @AuthenticationPrincipal Utente utente) {
        if (utente == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non autenticato");
        authService.changePassword(utente, req.getOldPassword(), req.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Elenco utenti (admin)", description = "Gestione account: non è per mondo, quindi riservata agli admin (il master di un mondo non gestisce gli account).")
    @GetMapping
    public ResponseEntity<List<UtenteAdminDTO>> list(@AuthenticationPrincipal Utente utente) {
        if (!authzService.isAdmin(utente))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato");
        return ResponseEntity.ok(authService.listUsers());
    }

    @Operation(summary = "Crea un nuovo utente senza password (admin, o master di almeno un mondo)")
    @PostMapping
    public ResponseEntity<UtenteAdminDTO> create(@Valid @RequestBody CreateUserRequest req,
                                                 @AuthenticationPrincipal Utente utente) {
        // account non legato a un mondo specifico (vedi list() sopra): basta essere MASTER di
        // ALMENO un mondo, come un admin — permesso indipendente da STATS/PAGINE.
        if (!authzService.isMasterOfAnyMondo(utente))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato");
        return ResponseEntity.ok(authService.createUser(req));
    }

    @Operation(summary = "Impersona un altro utente (solo admin)")
    @PostMapping("/{id}/impersonate")
    public ResponseEntity<LoginResponse> impersonate(@PathVariable Integer id,
                                                     @AuthenticationPrincipal Utente utente) {
        if (!authzService.isAdmin(utente))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo l'admin può impersonare");
        return ResponseEntity.ok(authService.impersonate(id));
    }
}
