package it.fin8.gdrsheet.service;

import it.fin8.gdrsheet.def.TipoPermessoMondo;
import it.fin8.gdrsheet.dto.CreateUserRequest;
import it.fin8.gdrsheet.dto.HomeDTO;
import it.fin8.gdrsheet.dto.LoginRequest;
import it.fin8.gdrsheet.dto.LoginResponse;
import it.fin8.gdrsheet.dto.UtenteAdminDTO;
import it.fin8.gdrsheet.entity.Party;
import it.fin8.gdrsheet.entity.PermessiParty;
import it.fin8.gdrsheet.entity.PermessiPersonaggi;
import it.fin8.gdrsheet.entity.Utente;
import it.fin8.gdrsheet.def.TipoPermessoPersonaggio;
import it.fin8.gdrsheet.repository.PartyRepository;
import it.fin8.gdrsheet.repository.PermessiMondoRepository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    private final UtenteRepository utenteRepository;
    private final PermessiPartyRepository permessiPartyRepository;
    private final PermessiPersonaggiRepository permessiPersonaggiRepository;
    private final PermessiMondoRepository permessiMondoRepository;
    private final PartyRepository partyRepository;
    private final PersonaggioRepository personaggioRepository;
    private final AuthzService authzService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UtenteRepository utenteRepository,
                       PermessiPartyRepository permessiPartyRepository,
                       PermessiPersonaggiRepository permessiPersonaggiRepository,
                       PermessiMondoRepository permessiMondoRepository,
                       PartyRepository partyRepository,
                       PersonaggioRepository personaggioRepository,
                       AuthzService authzService,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.utenteRepository = utenteRepository;
        this.permessiPartyRepository = permessiPartyRepository;
        this.permessiPersonaggiRepository = permessiPersonaggiRepository;
        this.permessiMondoRepository = permessiMondoRepository;
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

        boolean mustSet = isPasswordVuota(utente);
        String pwd = request.getPassword() == null ? "" : request.getPassword();
        // utente senza password: accesso consentito, dovrà impostarla
        if (!mustSet && !passwordMatches(utente, pwd)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenziali non valide");
        }

        return new LoginResponse(jwtService.generateToken(utente), toUtenteDTO(utente), mustSet);
    }

    private boolean isPasswordVuota(Utente utente) {
        return utente.getPassword() == null || utente.getPassword().isBlank();
    }

    /**
     * Cambia/imposta la password dell'utente loggato. Se l'utente non ha ancora
     * una password può impostarla senza fornire quella vecchia; altrimenti la
     * vecchia deve corrispondere.
     */
    @Transactional
    public void changePassword(Utente utente, String oldPassword, String newPassword) {
        if (utente == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non autenticato");
        if (newPassword == null || newPassword.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nuova password obbligatoria");

        Utente u = utenteRepository.findById(utente.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utente non trovato"));

        if (!isPasswordVuota(u) && !passwordMatches(u, oldPassword == null ? "" : oldPassword)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Password attuale non corretta");
        }

        u.setPassword(passwordEncoder.encode(newPassword));
        utenteRepository.save(u);
    }

    /**
     * Crea un nuovo utente senza password (potrà accedere e impostarla al primo login).
     * Riservato a master/admin (verifica nel controller).
     */
    @Transactional
    public UtenteAdminDTO createUser(CreateUserRequest req) {
        String username = req.getUsername() == null ? "" : req.getUsername().trim();
        if (username.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username obbligatorio");
        String name = req.getName() == null ? "" : req.getName().trim();
        if (name.isBlank()) name = username; // nome opzionale: default allo username
        if (utenteRepository.findByUsernameIgnoreCase(username).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username già esistente");

        String ruolo = (req.getRuolo() == null || req.getRuolo().isBlank())
                ? "GIOCATORE" : req.getRuolo().trim().toUpperCase();

        Utente u = new Utente();
        u.setUsername(username);
        u.setName(name);
        u.setRuolo(ruolo);
        u.setPassword(""); // nessuna password: la imposterà al primo accesso
        u = utenteRepository.save(u);
        return toAdminDTO(u);
    }

    @Transactional
    public LoginResponse.UtenteDTO updateProfile(Utente utente, String username, String name) {
        if (utente == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non autenticato");
        Utente u = utenteRepository.findById(utente.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utente non trovato"));
        if (username != null && !username.isBlank()) {
            String newUsername = username.trim();
            if (!newUsername.equalsIgnoreCase(u.getUsername()) &&
                    utenteRepository.findByUsernameIgnoreCase(newUsername).isPresent())
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Username già in uso");
            u.setUsername(newUsername);
        }
        if (name != null && !name.isBlank()) u.setName(name.trim());
        u = utenteRepository.save(u);
        return toUtenteDTO(u);
    }

    public List<UtenteAdminDTO> listUsers() {
        return utenteRepository.findAll().stream()
                .sorted(Comparator.comparing(Utente::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toAdminDTO)
                .toList();
    }

    /**
     * Genera un token per impersonare un altro utente. Riservato all'admin
     * (verifica nel controller).
     */
    public LoginResponse impersonate(Integer targetUserId) {
        Utente target = utenteRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utente non trovato"));
        return new LoginResponse(jwtService.generateToken(target), toUtenteDTO(target), isPasswordVuota(target));
    }

    private UtenteAdminDTO toAdminDTO(Utente u) {
        return new UtenteAdminDTO(u.getId(), u.getUsername(), u.getName(), u.getRuolo(), isPasswordVuota(u));
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
        // Party: l'admin vede tutti; gli altri quelli a cui sono associati direttamente PIÙ quelli
        // dei mondi di cui sono master (essere master di un mondo implica essere master di ogni
        // party di quel mondo, vedi AuthzService.isMasterParty) — con ruolo forzato a MASTER anche
        // se esisteva già un permesso esplicito con un ruolo più basso su quel party.
        List<HomeDTO.PartyHomeDTO> parties;
        if (authzService.isAdmin(utente)) {
            parties = partyRepository.findAll().stream()
                    .map(p -> partyDTO(p, "MASTER"))
                    .sorted(Comparator.comparing(HomeDTO.PartyHomeDTO::getNome, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } else {
            Map<Integer, HomeDTO.PartyHomeDTO> perParty = new LinkedHashMap<>();
            for (PermessiParty pp : permessiPartyRepository.findAllByIdUtente_Id(utente.getId())) {
                perParty.put(pp.getIdParty().getId(), toPartyDTO(pp));
            }
            for (var pm : permessiMondoRepository.findAllByIdUtente_IdAndPermesso(utente.getId(), TipoPermessoMondo.MASTER)) {
                for (Party p : partyRepository.findAllByMondo_IdOrderByNomeAsc(pm.getIdMondo().getId())) {
                    perParty.put(p.getId(), partyDTO(p, "MASTER"));
                }
            }
            parties = perParty.values().stream()
                    .sorted(Comparator.comparing(HomeDTO.PartyHomeDTO::getNome, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }

        // Personaggi: sempre e solo quelli direttamente associati all'utente
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
        return partyDTO(p.getIdParty(), p.getRuolo() != null ? p.getRuolo().name() : null);
    }

    /** Costruisce il DTO party includendo mondo (id/nome), per il filtro home per mondo selezionato. */
    private HomeDTO.PartyHomeDTO partyDTO(Party p, String ruolo) {
        var mondo = p.getMondo();
        return new HomeDTO.PartyHomeDTO(
                p.getId(),
                p.getNome(),
                ruolo,
                mondo != null ? mondo.getId() : null,
                mondo != null ? mondo.getDescrizione() : null
        );
    }

    private HomeDTO.PersonaggioHomeDTO toPersonaggioDTO(PermessiPersonaggi p) {
        var pg = p.getIdPersonaggio();
        var party = pg.getParty();
        var mondo = party != null ? party.getMondo() : null;
        return new HomeDTO.PersonaggioHomeDTO(
                pg.getId(),
                pg.getNome(),
                p.getPermesso() != null ? p.getPermesso().name() : null,
                party != null ? party.getId() : null,
                party != null ? party.getNome() : null,
                PartyService.tipoPersonaggio(pg),
                p.isPreferito(),
                mondo != null ? mondo.getId() : null,
                mondo != null ? mondo.getDescrizione() : null
        );
    }

    /** Preferito per l'utente corrente: null (no permesso ancora) conta come false. */
    public boolean getPreferito(Utente utente, Integer idPersonaggio) {
        return permessiPersonaggiRepository.findByIdUtente_IdAndIdPersonaggio_Id(utente.getId(), idPersonaggio)
                .map(PermessiPersonaggi::isPreferito)
                .orElse(false);
    }

    /**
     * Segna/rimuove il personaggio come preferito per l'utente corrente. Se l'utente non ha
     * ancora un permesso su questo personaggio (es. lo vede solo perché è nel party), gliene
     * crea uno da VISUALIZZATORE al volo: mettere la stellina non deve richiedere di passare
     * prima da un invito esplicito.
     */
    @Transactional
    public void setPreferito(Utente utente, Integer idPersonaggio, boolean preferito) {
        PermessiPersonaggi p = permessiPersonaggiRepository.findByIdUtente_IdAndIdPersonaggio_Id(utente.getId(), idPersonaggio)
                .orElseGet(() -> {
                    var pg = personaggioRepository.findPersonaggioById(idPersonaggio);
                    if (pg == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Personaggio non trovato");
                    PermessiPersonaggi nuovo = new PermessiPersonaggi();
                    nuovo.setIdUtente(utente);
                    nuovo.setIdPersonaggio(pg);
                    nuovo.setPermesso(TipoPermessoPersonaggio.VISUALIZZATORE);
                    return nuovo;
                });
        p.setPreferito(preferito);
        permessiPersonaggiRepository.save(p);
    }
}
