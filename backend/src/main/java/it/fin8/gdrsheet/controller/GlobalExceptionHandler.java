package it.fin8.gdrsheet.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Scrive la risposta di errore direttamente (ResponseEntity) invece di lasciare che
 * {@code ResponseStatusException} finisca in {@code HttpServletResponse.sendError(...)}.
 * <p>
 * {@code sendError} fa fare al servlet container un dispatch interno verso "/error" come
 * richiesta NUOVA, sulla quale l'intera catena di sicurezza di Spring viene rieseguita — ma
 * {@code JwtAuthFilter} (un {@code OncePerRequestFilter}) salta di default i dispatch di tipo
 * ERROR, quindi quella richiesta risulta non autenticata e l'authenticationEntryPoint in
 * {@link it.fin8.gdrsheet.config.SecurityConfig} scrive 401 al posto del vero status code.
 * Risultato: qualunque eccezione applicativa (es. un 404 "nessun oggetto trovato") arrivava al
 * frontend come 401, facendo scattare il logout automatico lato client.
 * <p>
 * Gestendo l'eccezione qui e restituendo un {@link ResponseEntity}, la risposta viene scritta
 * subito con lo status corretto, senza mai passare da sendError/dal redispatch su "/error".
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException e) {
        String messaggio = e.getReason() != null ? e.getReason() : e.getMessage();
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", messaggio));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Errore interno: " + e.getMessage()));
    }
}
