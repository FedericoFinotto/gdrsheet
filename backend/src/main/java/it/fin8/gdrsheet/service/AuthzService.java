package it.fin8.gdrsheet.service;

import it.fin8.gdrsheet.def.TipoPermessoMondo;
import it.fin8.gdrsheet.def.TipoPermessoPersonaggio;
import it.fin8.gdrsheet.entity.Mondo;
import it.fin8.gdrsheet.entity.Party;
import it.fin8.gdrsheet.entity.PermessiMondo;
import it.fin8.gdrsheet.entity.PermessiPersonaggi;
import it.fin8.gdrsheet.entity.Personaggio;
import it.fin8.gdrsheet.entity.Utente;
import it.fin8.gdrsheet.repository.PartyRepository;
import it.fin8.gdrsheet.repository.PermessiMondoRepository;
import it.fin8.gdrsheet.repository.PermessiPartyRepository;
import it.fin8.gdrsheet.repository.PermessiPersonaggiRepository;
import it.fin8.gdrsheet.repository.PersonaggioRepository;
import it.fin8.gdrsheet.repository.UtenteRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regole di accesso centralizzate.
 * <ul>
 *   <li>ADMIN/SUPERUSER: vede e può tutto, sempre.</li>
 *   <li>Personaggio senza proprietario: modificabile da chiunque sia membro
 *       di un party del personaggio.</li>
 *   <li>Personaggio con proprietario: modificabile solo dal proprietario e dal
 *       master del party.</li>
 * </ul>
 */
@Service
public class AuthzService {

    /**
     * Un tag ";P<id>;" o ";U<id>;": {@code (?=;)} invece di consumare il ';' finale, perché due
     * tag consecutivi condividono lo stesso ';' di confine (es. ";P1;P4;") — con un pattern che lo
     * consuma, un Matcher a match non sovrapposti (find()/results()) troverebbe solo il primo tag.
     */
    private static final Pattern PARTY_TAG_PATTERN = Pattern.compile(";P(\\d+)(?=;)");
    private static final Pattern UTENTE_TAG_PATTERN = Pattern.compile(";U(\\d+)(?=;)");

    private final PermessiPartyRepository permessiPartyRepository;
    private final PermessiPersonaggiRepository permessiPersonaggiRepository;
    private final PermessiMondoRepository permessiMondoRepository;
    private final PersonaggioRepository personaggioRepository;
    private final PartyRepository partyRepository;
    private final UtenteRepository utenteRepository;

    // Spring inietta un proxy request-scoped: sicuro in un @Service singleton
    @Autowired
    private HttpServletRequest request;

    public AuthzService(PermessiPartyRepository permessiPartyRepository,
                        PermessiPersonaggiRepository permessiPersonaggiRepository,
                        PermessiMondoRepository permessiMondoRepository,
                        PersonaggioRepository personaggioRepository,
                        PartyRepository partyRepository,
                        UtenteRepository utenteRepository) {
        this.permessiPartyRepository = permessiPartyRepository;
        this.permessiPersonaggiRepository = permessiPersonaggiRepository;
        this.permessiMondoRepository = permessiMondoRepository;
        this.personaggioRepository = personaggioRepository;
        this.partyRepository = partyRepository;
        this.utenteRepository = utenteRepository;
    }

    /** Ruolo reale dell'utente nel DB, indipendente dalla modalità corrente. */
    public boolean isRealAdmin(Utente utente) {
        if (utente == null || utente.getRuolo() == null) return false;
        String r = utente.getRuolo().trim().toUpperCase(Locale.ROOT);
        return r.equals("ADMIN") || r.equals("SUPERUSER");
    }

    /**
     * Ritorna true solo se l'utente è ADMIN *e* la modalità admin è attiva
     * (header X-Admin-Mode != "false"). Se l'header è assente o "true" → admin attivo.
     */
    public boolean isAdmin(Utente utente) {
        if (!isRealAdmin(utente)) return false;
        String header = request.getHeader("X-Admin-Mode");
        // header assente o qualsiasi valore diverso da "false" → admin attivo
        return !"false".equalsIgnoreCase(header);
    }

    /**
     * L'utente è master del MONDO indicato: gestisce il compendio di quel mondo (creare,
     * modificare, eliminare item; ricerca profonda; stat_default). A differenza del vecchio
     * ruolo globale "MASTER" sull'account (non più consultato qui), questo permesso è specifico
     * per mondo — un master del Mondo A non è automaticamente master del Mondo B. ADMIN resta
     * master di ogni mondo, sempre. Un mondoId nullo non abilita mai nulla (tranne per l'admin):
     * un'azione senza un mondo preciso non può essere autorizzata "per quel mondo".
     */
    public boolean isMasterMondo(Utente utente, Integer mondoId) {
        if (isAdmin(utente)) return true;
        if (utente == null || mondoId == null) return false;
        return permessiMondoRepository.existsByIdUtente_IdAndIdMondo_IdAndPermesso(
                utente.getId(), mondoId, TipoPermessoMondo.MASTER);
    }

    /**
     * L'utente può gestire le stat_default del MONDO indicato (StatController): permesso STATS,
     * indipendente da MASTER (vedi TipoPermessoMondo) — un MASTER non lo ottiene automaticamente,
     * va assegnato a parte.
     */
    public boolean isStatsMondo(Utente utente, Integer mondoId) {
        if (isAdmin(utente)) return true;
        if (utente == null || mondoId == null) return false;
        return permessiMondoRepository.existsByIdUtente_IdAndIdMondo_IdAndPermesso(
                utente.getId(), mondoId, TipoPermessoMondo.STATS);
    }

    /**
     * L'utente può configurare le "pagine" del MONDO indicato (MondoAdminController: tipi item
     * abilitati, card/campi dell'editor, catalogo scuole/liste incantesimi): permesso PAGINE,
     * indipendente da MASTER (vedi TipoPermessoMondo).
     */
    public boolean isPagineMondo(Utente utente, Integer mondoId) {
        if (isAdmin(utente)) return true;
        if (utente == null || mondoId == null) return false;
        return permessiMondoRepository.existsByIdUtente_IdAndIdMondo_IdAndPermesso(
                utente.getId(), mondoId, TipoPermessoMondo.PAGINE);
    }

    /**
     * Mondi su cui l'utente ha ESATTAMENTE il permesso indicato (nessun bypass admin: per un
     * admin va gestito a parte da chi chiama, es. "tutti i mondi"). Usato per unire i mondi
     * "posseduti" per un dato permesso con altre fonti (es. party di cui si è già membri) — vedi
     * PartyService.getMieiMondi, dove un MASTER deve poter creare la PRIMA party di un mondo anche
     * senza già farne parte.
     */
    public List<Mondo> mondiConPermesso(Utente utente, TipoPermessoMondo permesso) {
        if (utente == null) return List.of();
        return permessiMondoRepository.findAllByIdUtente_IdAndPermesso(utente.getId(), permesso).stream()
                .map(PermessiMondo::getIdMondo)
                .toList();
    }

    /**
     * L'utente è MASTER di almeno un mondo (qualunque): usato per azioni non legate a un mondo
     * specifico ma comunque riservate a chi ha "un minimo" di potere gestionale, es. creare un
     * nuovo account (UserController) — oggi riservato ai soli admin, esteso anche a chi è master
     * di almeno un mondo, senza che l'account creato sia legato a nessun mondo in particolare.
     */
    public boolean isMasterOfAnyMondo(Utente utente) {
        if (isAdmin(utente)) return true;
        if (utente == null) return false;
        return !permessiMondoRepository.findAllByIdUtente_IdAndPermesso(utente.getId(), TipoPermessoMondo.MASTER).isEmpty();
    }

    /**
     * L'utente ha ALMENO UN permesso su ALMENO UN mondo (MASTER, STATS o PAGINE indifferentemente):
     * usato per la lettura di cataloghi globali (liste/domini incantesimi, scuole/sottoscuole...)
     * che servono a decidere cosa abilitare nel proprio mondo, qualunque sia il permesso posseduto
     * su di esso.
     */
    public boolean hasAnyPermessoMondo(Utente utente) {
        if (isAdmin(utente)) return true;
        if (utente == null) return false;
        return !permessiMondoRepository.findAllByIdUtente_Id(utente.getId()).isEmpty();
    }

    /**
     * L'utente è master del MONDO a cui appartiene questo party (vedi {@link #isMasterMondo}):
     * essere master di un mondo implica essere master di ogni party di quel mondo, anche senza
     * un permesso esplicito su quel party. Un party senza mondo (mondo_id nullo) non abilita mai
     * nulla per questa via.
     */
    private boolean isMasterMondoDelParty(Utente utente, Integer partyId) {
        if (partyId == null) return false;
        return partyRepository.findById(partyId)
                .map(Party::getMondo)
                .map(Mondo::getId)
                .map(mondoId -> isMasterMondo(utente, mondoId))
                .orElse(false);
    }

    /**
     * L'utente è membro (qualsiasi ruolo) del party — oppure master del mondo del party, che
     * implica almeno la membership (vedi {@link #isMasterMondoDelParty}).
     */
    public boolean isMembroParty(Utente utente, Integer partyId) {
        if (isAdmin(utente)) return true;
        if (partyId == null) return false;
        boolean membro = permessiPartyRepository.findAllByIdUtente_Id(utente.getId()).stream()
                .anyMatch(p -> Objects.equals(p.getIdParty().getId(), partyId));
        return membro || isMasterMondoDelParty(utente, partyId);
    }

    /**
     * L'utente è master del party: SOLO perché è master del mondo a cui il party appartiene (vedi
     * {@link #isMasterMondoDelParty}) — non esiste più un ruolo "master" a livello di singolo
     * party (eliminato: chi viene aggiunto a un party è sempre e solo un membro, vedi
     * PartyService.addMembro/createParty). Gestire un party è quindi interamente una responsabilità
     * del master del suo mondo.
     */
    public boolean isMasterParty(Utente utente, Integer partyId) {
        if (isAdmin(utente)) return true;
        return isMasterMondoDelParty(utente, partyId);
    }

    public boolean canEditPersonaggio(Utente utente, Integer personaggioId) {
        if (isAdmin(utente)) return true;
        if (personaggioId == null) return false;
        Personaggio pg = personaggioRepository.findPersonaggioById(personaggioId);
        if (pg == null) return false;
        Integer partyId = pg.getParty() != null ? pg.getParty().getId() : null;

        List<PermessiPersonaggi> proprietari = permessiPersonaggiRepository.findAllByIdPersonaggio_Id(personaggioId).stream()
                .filter(p -> TipoPermessoPersonaggio.PROPRIETARIO.equals(p.getPermesso()))
                .toList();

        if (proprietari.isEmpty()) {
            // senza proprietario: chiunque sia membro del party
            return isMembroParty(utente, partyId);
        }
        // con proprietario: proprietario stesso o master del party
        boolean isProprietario = proprietari.stream()
                .anyMatch(p -> Objects.equals(p.getIdUtente().getId(), utente.getId()));
        return isProprietario || isMasterParty(utente, partyId);
    }

    /**
     * L'utente è proprietario del personaggio.
     */
    public boolean isProprietarioPersonaggio(Utente utente, Integer personaggioId) {
        if (utente == null || personaggioId == null) return false;
        return permessiPersonaggiRepository.findAllByIdPersonaggio_Id(personaggioId).stream()
                .filter(p -> TipoPermessoPersonaggio.PROPRIETARIO.equals(p.getPermesso()))
                .anyMatch(p -> Objects.equals(p.getIdUtente().getId(), utente.getId()));
    }

    /**
     * Regola di visibilità di un item (o di una nota: stesso campo, stesso significato, solo un
     * livello più granulare) nel contesto di un personaggio:
     * <ul>
     *   <li>vuoto/null: visibile a tutti i membri del party (comportamento normale);</li>
     *   <li>OWNER: solo proprietario, master e admin;</li>
     *   <li>MASTER: solo master e admin;</li>
     *   <li>una o più coppie {@code ;P<idParty>;} e/o {@code ;U<idUtente>;} (es. {@code ";P3;P7;U12;"}):
     *       visibile solo a chi appartiene a uno di quei party (qualunque ruolo, non dedotto dal
     *       personaggio di contesto: un INFO/QUEST di ambito MONDO o PARTY non ha un personaggio
     *       proprietario da cui risalire, ma chi guarda appartiene comunque a un party) o è
     *       quell'utente. I delimitatori ';' su entrambi i lati evitano falsi positivi tra id che
     *       condividono le stesse cifre (es. party 1 non deve combaciare con party 12).</li>
     * </ul>
     */
    public boolean canViewVisibilita(Utente utente, Personaggio personaggio, String visibilita) {
        if (visibilita == null || visibilita.isBlank()) return true;
        if (isAdmin(utente)) return true;
        Integer partyId = (personaggio != null && personaggio.getParty() != null)
                ? personaggio.getParty().getId() : null;
        boolean master = isMasterParty(utente, partyId);
        String v = visibilita.trim().toUpperCase(Locale.ROOT);
        if (v.equals("MASTER")) return master;
        if (v.equals("OWNER")) {
            return master || (personaggio != null && isProprietarioPersonaggio(utente, personaggio.getId()));
        }
        if (v.contains(";P") || v.contains(";U")) {
            if (utente == null) return false;
            // (?=;) invece di consumare il ';' finale: due tag consecutivi condividono lo stesso
            // ';' di confine (es. ";P1;P4;"), e Matcher.find() non produce match sovrapposti — con
            // un pattern che CONSUMA il ';' condiviso, il secondo tag non verrebbe mai trovato.
            Matcher m = PARTY_TAG_PATTERN.matcher(v);
            while (m.find()) {
                if (isMembroParty(utente, Integer.valueOf(m.group(1)))) return true;
            }
            return UTENTE_TAG_PATTERN.matcher(v).results()
                    .anyMatch(r -> Integer.valueOf(r.group(1)).equals(utente.getId()));
        }
        return true; // valore non riconosciuto: visibile
    }

    /**
     * Etichette leggibili per il campo "visibilita" di una nota/item, una per party/utente
     * taggato (nomi risolti da id) — per il formato a tag ";P<id>;.../U<id>;". Per OWNER/MASTER
     * torna una singola etichetta descrittiva; per vuoto o non riconosciuto (stessa semantica di
     * canViewVisibilita: visibile a tutti) torna sempre "Tutti", mai una lista vuota — il chip
     * compare sempre, coerente per ogni nota indipendentemente dalla sua visibilità. Usata SOLO
     * per la visualizzazione, non per l'autorizzazione.
     */
    public List<String> descriviVisibilitaChips(String visibilita) {
        if (visibilita == null || visibilita.isBlank()) return List.of("Tutti");
        String v = visibilita.trim().toUpperCase(Locale.ROOT);
        if (v.equals("OWNER")) return List.of("Solo il proprietario del personaggio");
        if (v.equals("MASTER")) return List.of("Solo il Master");
        if (!v.contains(";P") && !v.contains(";U")) return List.of("Tutti");

        List<String> chips = new ArrayList<>();
        Matcher pm = PARTY_TAG_PATTERN.matcher(v);
        while (pm.find()) {
            Integer partyId = Integer.valueOf(pm.group(1));
            chips.add(partyRepository.findById(partyId).map(p -> p.getNome()).orElse("Party #" + partyId));
        }
        Matcher um = UTENTE_TAG_PATTERN.matcher(v);
        while (um.find()) {
            Integer utenteId = Integer.valueOf(um.group(1));
            chips.add(utenteRepository.findById(utenteId).map(u -> u.getName()).orElse("Utente #" + utenteId));
        }
        return chips;
    }

    /**
     * Classe di visibilità di un utente rispetto a un personaggio: determina interamente cosa
     * quell'utente vede (vedi canViewVisibilita) e il controllo "solo admin" sugli item
     * strutturali. Usata per chiavare la cache /items per "ruolo effettivo" invece che per
     * singolo utente: due utenti nella stessa classe vedono esattamente lo stesso ItemsDTO,
     * quindi possono condividere la stessa entry di cache (al massimo 4 varianti per
     * personaggio: ADMIN, MASTER, OWNER, GIOCATORE — invece di una per utente).
     */
    public String visibilityClass(Utente utente, Integer personaggioId, Integer partyId) {
        if (isAdmin(utente)) return "ADMIN";
        if (isMasterParty(utente, partyId)) return "MASTER";
        if (isProprietarioPersonaggio(utente, personaggioId)) return "OWNER";
        return "GIOCATORE";
    }

    public void assertCanEditPersonaggio(Utente utente, Integer personaggioId) {
        if (!canEditPersonaggio(utente, personaggioId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Non puoi modificare questo personaggio");
        }
    }
}
