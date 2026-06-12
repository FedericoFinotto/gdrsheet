package it.fin8.gdrsheet.service;

import it.fin8.gdrsheet.dto.HomeDTO;
import it.fin8.gdrsheet.dto.LoginRequest;
import it.fin8.gdrsheet.dto.LoginResponse;
import it.fin8.gdrsheet.entity.PermessiParty;
import it.fin8.gdrsheet.entity.PermessiPersonaggi;
import it.fin8.gdrsheet.entity.Utente;
import it.fin8.gdrsheet.def.TipoPermessoPersonaggio;
import it.fin8.gdrsheet.repository.PartyRepository;
import it.fin8.gdrsheet.repository.PermessiPartyRepository;
import it.fin8.gdrsheet.repository.PermessiPersonaggiRepository;
import it.fin8.gdrsheet.repository.PersonaggioRepository;
import it.fin8.gdrsheet.repository.UtenteRepository;
import it.fin8.gdrsheet.security.JwtService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
public class AuthService {

    private final UtenteRepository utenteRepository;
    private final PermessiPartyRepository permessiPartyRepository;
    private final PermessiPersonaggiRepository permessiPersonaggiRepository;
    private final PartyRepository partyRepository;
    private final PersonaggioRepository personaggioRepository;
    private final AuthzService authzService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UtenteRepository utenteRepository,
                       PermessiPartyRepository permessiPartyRepository,
                       PermessiPersonaggiRepository permessiPersonaggiRepository,
                       PartyRepository partyRepository,
                       PersonaggioRepository personaggioRepository,
                       AuthzService authzService,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.utenteRepository = utenteRepository;
        this.permessiPartyRepository = permessiPartyRepository;
        this.permessiPersonaggiRepository = permessiPersonaggiRepository;
        this.partyRepository = partyRepository;
        this.personaggioRepository = personaggioRepository;
        this.authzService = authzService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Utente utente = utenteRepository.findByUsernameIgnoreCase(request.getUsername().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenziali non valide"));

        if (!passwordMatches(utente, request.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenziali non valide");
        }

        return new LoginResponse(jwtService.generateToken(utente), toUtenteDTO(utente));
    }

    /**
     * Verifica la password. Le password legacy salvate in chiaro vengono
     * accettate e ri-salvate hashate (upgrade trasparente al primo login).
     */
    private boolean passwordMatches(Utente utente, String rawPassword) {
        String stored = utente.getPassword();
        if (stored == null || stored.isEmpty()) return false;

        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, stored);
        }

        // legacy: confronto in chiaro + upgrade a bcrypt
        if (stored.equals(rawPassword)) {
            utente.setPassword(passwordEncoder.encode(rawPassword));
            utenteRepository.save(utente);
            return true;
        }
        return false;
    }

    public HomeDTO getHome(Utente utente) {
        // ADMIN/SUPERUSER: vede tutti i party e tutti i personaggi
        if (authzService.isAdmin(utente)) {
            List<HomeDTO.PartyHomeDTO> parties = partyRepository.findAll().stream()
                    .map(p -> new HomeDTO.PartyHomeDTO(p.getId(), p.getNome(), "MASTER"))
                    .sorted(Comparator.comparing(HomeDTO.PartyHomeDTO::getNome, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            List<HomeDTO.PersonaggioHomeDTO> personaggi = personaggioRepository.findAll().stream()
                    .map(pg -> new HomeDTO.PersonaggioHomeDTO(
                            pg.getId(),
                            pg.getNome(),
                            TipoPermessoPersonaggio.PROPRIETARIO.name(),
                            pg.getParty() != null ? pg.getParty().getId() : null,
                            pg.getParty() != null ? pg.getParty().getNome() : null,
                            PartyService.tipoPersonaggio(pg)))
                    .sorted(Comparator.comparing(HomeDTO.PersonaggioHomeDTO::getNome, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            return new HomeDTO(toUtenteDTO(utente), parties, personaggi);
        }

        List<HomeDTO.PartyHomeDTO> parties = permessiPartyRepository.findAllByIdUtente_Id(utente.getId()).stream()
                .map(this::toPartyDTO)
                .sorted(Comparator.comparing(HomeDTO.PartyHomeDTO::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<HomeDTO.PersonaggioHomeDTO> personaggi = permessiPersonaggiRepository.findAllByIdUtente_Id(utente.getId()).stream()
                .map(this::toPersonaggioDTO)
                .sorted(Comparator.comparing(HomeDTO.PersonaggioHomeDTO::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new HomeDTO(toUtenteDTO(utente), parties, personaggi);
    }

    private LoginResponse.UtenteDTO toUtenteDTO(Utente u) {
        return new LoginResponse.UtenteDTO(u.getId(), u.getUsername(), u.getName(), u.getRuolo());
    }

    private HomeDTO.PartyHomeDTO toPartyDTO(PermessiParty p) {
        return new HomeDTO.PartyHomeDTO(
                p.getIdParty().getId(),
                p.getIdParty().getNome(),
                p.getRuolo() != null ? p.getRuolo().name() : null
        );
    }

    private HomeDTO.PersonaggioHomeDTO toPersonaggioDTO(PermessiPersonaggi p) {
        var pg = p.getIdPersonaggio();
        var party = pg.getParty();
        return new HomeDTO.PersonaggioHomeDTO(
                pg.getId(),
                pg.getNome(),
                p.getPermesso() != null ? p.getPermesso().name() : null,
                party != null ? party.getId() : null,
                party != null ? party.getNome() : null,
                PartyService.tipoPersonaggio(pg)
        );
    }
}
