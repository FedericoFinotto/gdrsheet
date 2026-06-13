package it.fin8.gdrsheet.service;

import it.fin8.gdrsheet.def.TipoPermessoPersonaggio;
import it.fin8.gdrsheet.def.TipoRuoloParty;
import it.fin8.gdrsheet.entity.PermessiPersonaggi;
import it.fin8.gdrsheet.entity.Personaggio;
import it.fin8.gdrsheet.entity.Utente;
import it.fin8.gdrsheet.repository.PermessiPartyRepository;
import it.fin8.gdrsheet.repository.PermessiPersonaggiRepository;
import it.fin8.gdrsheet.repository.PersonaggioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

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

    private final PermessiPartyRepository permessiPartyRepository;
    private final PermessiPersonaggiRepository permessiPersonaggiRepository;
    private final PersonaggioRepository personaggioRepository;

    public AuthzService(PermessiPartyRepository permessiPartyRepository,
                        PermessiPersonaggiRepository permessiPersonaggiRepository,
                        PersonaggioRepository personaggioRepository) {
        this.permessiPartyRepository = permessiPartyRepository;
        this.permessiPersonaggiRepository = permessiPersonaggiRepository;
        this.personaggioRepository = personaggioRepository;
    }

    public boolean isAdmin(Utente utente) {
        if (utente == null || utente.getRuolo() == null) return false;
        String r = utente.getRuolo().trim().toUpperCase(Locale.ROOT);
        return r.equals("ADMIN") || r.equals("SUPERUSER");
    }

    /**
     * Ruolo applicativo MASTER (o admin): richiesto per le operazioni
     * distruttive sul compendio, come la cancellazione degli item.
     */
    public boolean isMasterOrAdmin(Utente utente) {
        if (isAdmin(utente)) return true;
        if (utente == null || utente.getRuolo() == null) return false;
        return utente.getRuolo().trim().toUpperCase(Locale.ROOT).equals("MASTER");
    }

    /**
     * L'utente è membro (qualsiasi ruolo) del party.
     */
    public boolean isMembroParty(Utente utente, Integer partyId) {
        if (isAdmin(utente)) return true;
        if (partyId == null) return false;
        return permessiPartyRepository.findAllByIdUtente_Id(utente.getId()).stream()
                .anyMatch(p -> Objects.equals(p.getIdParty().getId(), partyId));
    }

    /**
     * L'utente è master del party.
     */
    public boolean isMasterParty(Utente utente, Integer partyId) {
        if (isAdmin(utente)) return true;
        if (partyId == null) return false;
        return permessiPartyRepository.findAllByIdUtente_Id(utente.getId()).stream()
                .anyMatch(p -> Objects.equals(p.getIdParty().getId(), partyId)
                        && TipoRuoloParty.MASTER.equals(p.getRuolo()));
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

    public void assertCanEditPersonaggio(Utente utente, Integer personaggioId) {
        if (!canEditPersonaggio(utente, personaggioId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Non puoi modificare questo personaggio");
        }
    }
}
