package it.fin8.gdrsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.fin8.gdrsheet.dto.ChangePasswordRequest;
import it.fin8.gdrsheet.dto.CreateUserRequest;
import it.fin8.gdrsheet.dto.LoginResponse;
import it.fin8.gdrsheet.dto.UtenteAdminDTO;
import it.fin8.gdrsheet.entity.Utente;
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

    public UserController(AuthService authService, AuthzService authzService) {
        this.authService = authService;
        this.authzService = authzService;
    }

    @Operation(summary = "Cambia/imposta la password dell'utente loggato")
    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req,
                                               @AuthenticationPrincipal Utente utente) {
        if (utente == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non autenticato");
        authService.changePassword(utente, req.getOldPassword(), req.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Elenco utenti (master/admin)")
    @GetMapping
    public ResponseEntity<List<UtenteAdminDTO>> list(@AuthenticationPrincipal Utente utente) {
        if (!authzService.isMasterOrAdmin(utente))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato");
        return ResponseEntity.ok(authService.listUsers());
    }

    @Operation(summary = "Crea un nuovo utente senza password (master/admin)")
    @PostMapping
    public ResponseEntity<UtenteAdminDTO> create(@Valid @RequestBody CreateUserRequest req,
                                                 @AuthenticationPrincipal Utente utente) {
        if (!authzService.isMasterOrAdmin(utente))
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
