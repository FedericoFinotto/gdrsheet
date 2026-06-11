package it.fin8.gdrsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import it.fin8.gdrsheet.dto.HomeDTO;
import it.fin8.gdrsheet.dto.LoginRequest;
import it.fin8.gdrsheet.dto.LoginResponse;
import it.fin8.gdrsheet.entity.Utente;
import it.fin8.gdrsheet.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Login",
            description = "Autentica l'utente e restituisce il token JWT"
    )
    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(
            summary = "Home dell'utente loggato",
            description = "Party (con ruolo MASTER/GIOCATORE) e personaggi (PROPRIETARIO/VISUALIZZATORE) dell'utente"
    )
    @GetMapping("/home")
    public ResponseEntity<HomeDTO> home(@AuthenticationPrincipal Utente utente) {
        return ResponseEntity.ok(authService.getHome(utente));
    }
}
