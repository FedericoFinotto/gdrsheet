package it.fin8.gdrsheet.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import it.fin8.gdrsheet.dto.CalcoloRequest;
import it.fin8.gdrsheet.dto.CalcoloResponse;
import it.fin8.gdrsheet.service.CalcoloService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calcolo")
public class CalcoloController {

    private final CalcoloService calcoloService;

    public CalcoloController(CalcoloService calcoloService) {
        this.calcoloService = calcoloService;
    }

    @Operation(
            summary = "Esegue un calcolo basandosi su una formula e i dati passati",
            description = "Esegue un calcolo basandosi su una formula e i dati passati"
    )
    @PostMapping("/formula")
    public ResponseEntity<CalcoloResponse> calcola(
            @Parameter(description = "Corpo della richiesta con formula e dati del personaggio", required = true)
            @Valid @RequestBody CalcoloRequest request
    ) {

        // fai il calcolo (un solo giro di costruzione delle mappe per risultato/formulaRisolta/
        // formulaLeggibile, invece di tre — vedi CalcoloService.calcolaCompleto)
        CalcoloResponse resp = calcoloService.calcolaCompleto(request.getFormula(), request.getDatiPersonaggio());

        return ResponseEntity.ok(resp);
    }
}
