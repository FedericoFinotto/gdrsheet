package it.fin8.gdrsheet.service;

import it.fin8.gdrsheet.config.Constants;
import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.def.TipoModificatore;
import it.fin8.gdrsheet.def.TipoPermessoPersonaggio;
import it.fin8.gdrsheet.def.TipoRuoloParty;
import it.fin8.gdrsheet.dto.*;
import it.fin8.gdrsheet.entity.*;
import it.fin8.gdrsheet.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PartyService {

    private static final List<String> STAT_MONETE = List.of("MR", "MA", "MO", "MP");
    private static final String LABEL_TIPO_PERSONAGGIO = "TIPO_PERSONAGGIO";
    private static final Set<TipoItem> TIPI_INVENTARIO = Set.of(
            TipoItem.OGGETTO, TipoItem.CONSUMABILE, TipoItem.ARMA, TipoItem.MUNIZIONE, TipoItem.EQUIPAGGIAMENTO);

    private static final String NOME_ITEM_BORSELLINO = "Borsellino";

    private final PermessiPartyRepository permessiPartyRepository;
    private final PermessiPersonaggiRepository permessiPersonaggiRepository;
    private final PersonaggioRepository personaggioRepository;
    private final ModificatoreRepository modificatoreRepository;
    private final ItemRepository itemRepository;
    private final ItemLabelRepository itemLabelRepository;
    private final CollegamentoRepository collegamentoRepository;
    private final ItemService itemService;
    private final PartyRepository partyRepository;
    private final MondoRepository mondoRepository;
    private final UtenteRepository utenteRepository;
    private final AuthzService authzService;
    private final GruppoRepository gruppoRepository;
    private final EntityManager em;

    public PartyService(PermessiPartyRepository permessiPartyRepository,
                        PermessiPersonaggiRepository permessiPersonaggiRepository,
                        PersonaggioRepository personaggioRepository,
                        ModificatoreRepository modificatoreRepository,
                        ItemRepository itemRepository,
                        ItemLabelRepository itemLabelRepository,
                        CollegamentoRepository collegamentoRepository,
                        ItemService itemService,
                        PartyRepository partyRepository,
                        MondoRepository mondoRepository,
                        UtenteRepository utenteRepository,
                        AuthzService authzService,
                        GruppoRepository gruppoRepository,
                        EntityManager em) {
        this.permessiPartyRepository = permessiPartyRepository;
        this.permessiPersonaggiRepository = permessiPersonaggiRepository;
        this.personaggioRepository = personaggioRepository;
        this.modificatoreRepository = modificatoreRepository;
        this.itemRepository = itemRepository;
        this.itemLabelRepository = itemLabelRepository;
        this.collegamentoRepository = collegamentoRepository;
        this.itemService = itemService;
        this.partyRepository = partyRepository;
        this.mondoRepository = mondoRepository;
        this.utenteRepository = utenteRepository;
        this.authzService = authzService;
        this.gruppoRepository = gruppoRepository;
        this.em = em;
    }

    public PartyDetailDTO getPartyDetail(Integer partyId, Utente utente) {
        PermessiParty permesso = permessiPartyRepository.findAllByIdUtente_Id(utente.getId()).stream()
                .filter(p -> Objects.equals(p.getIdParty().getId(), partyId))
                .findFirst()
                .orElse(null);
        if (permesso == null && !authzService.isAdmin(utente))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non fai parte di questo party");

        Party party = permesso != null ? permesso.getIdParty()
                : partyRepository.findById(partyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Party non trovato"));
        String ruoloUtente = permesso != null && permesso.getRuolo() != null
                ? permesso.getRuolo().name()
                : (authzService.isAdmin(utente) ? "MASTER" : null);

        List<Personaggio> membri = personaggioRepository.findAllByParty_IdOrderByNomeAsc(partyId);

        // personaggi di cui l'utente corrente è proprietario
        Set<Integer> posseduti = permessiPersonaggiRepository.findAllByIdUtente_Id(utente.getId()).stream()
                .filter(p -> TipoPermessoPersonaggio.PROPRIETARIO.equals(p.getPermesso()))
                .map(p -> p.getIdPersonaggio().getId())
                .collect(Collectors.toSet());

        List<PartyDetailDTO.PersonaggioSoldiDTO> personaggi = new ArrayList<>();
        PartyDetailDTO.SoldiDTO somma = new PartyDetailDTO.SoldiDTO(0, 0, 0, 0);
        double pesoTotale = 0;
        double pesoMonete = 0;

        for (Personaggio pg : membri) {
            String tipoPg = tipoPersonaggio(pg);
            // totale mostrato = soldi personali + conto banca del giocatore;
            // per le banche invece la somma di tutti i loro conti correnti
            PartyDetailDTO.SoldiDTO personali = calcolaSoldi(pg.getId());
            PartyDetailDTO.SoldiDTO soldi;
            if (Constants.TIPO_PERSONAGGIO_BANCA.equals(tipoPg)) {
                soldi = sommaContiBanca(pg);
            } else {
                soldi = new PartyDetailDTO.SoldiDTO(
                        personali.getMr(), personali.getMa(), personali.getMo(), personali.getMp());
                soldi.add(calcolaSoldiConto(Constants.LABEL_CC_GIOCATORE_PREFIX + pg.getId()));
            }

            somma.add(soldi);
            // il peso conta solo le monete trasportate (personali)
            pesoMonete += calcolaPesoMonete(personali);
            // Peso effettivo dalla cache (personaggio_label PESO_EFFETTIVO, scritta all'apertura scheda).
            // Se il personaggio non è mai stato aperto la label manca: peso 0 (non verrà mostrato).
            double peso = 0;
            String pesoEffRaw = personaggioLabel(pg, Constants.LABEL_PESO_EFFETTIVO);
            if (pesoEffRaw != null) {
                try { peso = Double.parseDouble(pesoEffRaw.trim().replace(',', '.')); }
                catch (NumberFormatException ignored) {}
            }
            pesoTotale += peso;
            String gruppoRaw = personaggioLabel(pg, Constants.LABEL_GRUPPO);
            Integer gruppoId = null;
            try { if (gruppoRaw != null) gruppoId = Integer.parseInt(gruppoRaw.trim()); } catch (NumberFormatException ignored) {}
            boolean capogruppo = "1".equals(personaggioLabel(pg, Constants.LABEL_CAPOGRUPPO));
            // Livello atteso (label) e numero di livelli effettivi (item LIVELLO, escluso il livello 0)
            Integer livello = null;
            String livelloRaw = personaggioLabel(pg, Constants.LABEL_LIVELLO);
            try { if (livelloRaw != null) livello = Integer.parseInt(livelloRaw.trim()); } catch (NumberFormatException ignored) {}
            int numLivelli = pg.getItems() == null ? 0 : (int) pg.getItems().stream()
                    .filter(i -> TipoItem.LIVELLO.equals(i.getTipo()))
                    .filter(i -> !Boolean.TRUE.equals(i.isDisabled()))   // i livelli disabilitati non contano
                    .filter(i -> !"0".equals(i.getLabel(Constants.ITEM_LIVELLO_LVL)))
                    .count();
            personaggi.add(new PartyDetailDTO.PersonaggioSoldiDTO(
                    pg.getId(),
                    pg.getNome(),
                    soldi,
                    tipoPg,
                    posseduti.contains(pg.getId()),
                    peso,
                    gruppoId,
                    capogruppo,
                    livello,
                    numLivelli
            ));
        }

        // conto banca intestato al party
        somma.add(calcolaSoldiConto(Constants.LABEL_CC_PARTY_PREFIX + partyId));

        List<PartyDetailDTO.GruppoInfoDTO> gruppi = gruppoRepository.findAllByParty_IdOrderByNomeAsc(partyId).stream()
                .map(g -> new PartyDetailDTO.GruppoInfoDTO(g.getId(), g.getNome()))
                .toList();

        return new PartyDetailDTO(
                party.getId(),
                party.getNome(),
                ruoloUtente,
                personaggi,
                gruppi,
                somma,
                Math.round(pesoTotale * 100) / 100.0,
                Math.round(pesoMonete * 100) / 100.0
        );
    }

    /**
     * Item di inventario dei membri del party, filtrati e paginati.
     */
    public PageDTO<PartyItemDTO> getPartyItems(Integer partyId, Utente utente,
                                               String nome, String tipo, int page, int size) {
        verificaMembership(partyId, utente);

        List<Personaggio> membri = personaggioRepository.findAllByParty_IdOrderByNomeAsc(partyId);
        List<PartyItemDTO> tutti = new ArrayList<>();

        for (Personaggio pg : membri) {
            Item fromCompendio = itemRepository.findItemByNomeAndPersonaggio_Id(Constants.ITEM_FROM_COMPENDIO, pg.getId());
            if (fromCompendio == null) continue;
            for (Collegamento link : collegamentoRepository.findAllByItemSource_Id(fromCompendio.getId())) {
                Item itm = link.getItemTarget();
                if (!TIPI_INVENTARIO.contains(itm.getTipo())) continue;
                int quantita = parseQta(itm.getLabel(Constants.LABEL_QTA));
                double peso = Math.round(parsePeso(itm.getLabel(Constants.LABEL_PESO)) * quantita * 100) / 100.0;
                tutti.add(new PartyItemDTO(
                        itm.getId(),
                        itm.getNome(),
                        itm.getTipo().name(),
                        peso,
                        quantita,
                        pg.getId(),
                        pg.getNome(),
                        Boolean.TRUE.equals(itm.isDisabled()) || Boolean.TRUE.equals(link.isDisabled())
                ));
            }
        }

        // filtri (case-insensitive)
        String nomeFiltro = nome == null ? "" : nome.trim().toLowerCase(Locale.ROOT);
        List<PartyItemDTO> filtrati = tutti.stream()
                .filter(i -> nomeFiltro.isEmpty() || i.getNome().toLowerCase(Locale.ROOT).contains(nomeFiltro))
                .filter(i -> tipo == null || tipo.isBlank() || i.getTipo().equalsIgnoreCase(tipo))
                .sorted((a, b) -> a.getNome().compareToIgnoreCase(b.getNome()))
                .toList();

        // paginazione in memoria (gli inventari di un party sono piccoli)
        int sz = Math.max(1, size);
        int totalPages = Math.max(1, (int) Math.ceil(filtrati.size() / (double) sz));
        int p = Math.min(Math.max(0, page), totalPages - 1);
        int from = p * sz;
        int to = Math.min(from + sz, filtrati.size());
        List<PartyItemDTO> content = from >= filtrati.size() ? List.of() : filtrati.subList(from, to);

        return new PageDTO<>(content, p, sz, filtrati.size(), totalPages);
    }

    /**
     * Sposta un item dall'inventario di un personaggio a un altro:
     * il collegamento passa dal FromCompendio di origine a quello di destinazione.
     */
    @Transactional
    public void giveItem(GiveItemRequest request, Utente utente) {
        Personaggio from = personaggioRepository.findPersonaggioById(request.getFromPersonaggioId());
        Personaggio to = personaggioRepository.findPersonaggioById(request.getToPersonaggioId());
        if (from == null || to == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Personaggio non trovato");
        if (from.getParty() == null || to.getParty() == null
                || !Objects.equals(from.getParty().getId(), to.getParty().getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "I personaggi non sono nello stesso party");

        verificaMembership(from.getParty().getId(), utente);

        Item fromCompendio = itemRepository.findItemByNomeAndPersonaggio_Id(Constants.ITEM_FROM_COMPENDIO, from.getId());
        if (fromCompendio == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventario di origine non trovato");

        List<Collegamento> links = collegamentoRepository.findAllByItemSource_Id(fromCompendio.getId()).stream()
                .filter(c -> Objects.equals(c.getItemTarget().getId(), request.getItemId()))
                .toList();
        if (links.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "L'item non è nell'inventario di origine");

        Item target = links.get(0).getItemTarget();
        collegamentoRepository.deleteAll(links);

        Item toCompendio = itemService.ensureFromCompendio(to.getId());
        boolean giaPresente = collegamentoRepository.findAllByItemSource_Id(toCompendio.getId()).stream()
                .anyMatch(c -> Objects.equals(c.getItemTarget().getId(), target.getId()));
        if (!giaPresente) {
            Collegamento nuovo = new Collegamento();
            nuovo.setItemSource(toCompendio);
            nuovo.setItemTarget(target);
            collegamentoRepository.save(nuovo);
        }
    }

    /* =====================================================================
     * Creazione party e personaggi
     * ===================================================================== */

    /**
     * I mondi a cui il master ha accesso (dedotti dai suoi party).
     */
    public List<MondoDTO> getMieiMondi(Utente utente) {
        if (authzService.isAdmin(utente)) {
            return mondoRepository.findAll().stream()
                    .map(m -> new MondoDTO(m.getId(), m.getDescrizione(),
                            m.getSistema() != null ? m.getSistema().getId() : null,
                            m.getSistema() != null ? m.getSistema().getDescrizione() : null))
                    .sorted((a, b) -> a.descrizione().compareToIgnoreCase(b.descrizione()))
                    .toList();
        }
        Map<Integer, MondoDTO> mondi = new LinkedHashMap<>();
        for (PermessiParty pp : permessiPartyRepository.findAllByIdUtente_Id(utente.getId())) {
            Party party = pp.getIdParty();
            if (party != null && party.getMondo() != null) {
                Mondo m = party.getMondo();
                mondi.putIfAbsent(m.getId(), new MondoDTO(m.getId(), m.getDescrizione(),
                        m.getSistema() != null ? m.getSistema().getId() : null,
                        m.getSistema() != null ? m.getSistema().getDescrizione() : null));
            }
        }
        return mondi.values().stream()
                .sorted((a, b) -> a.descrizione().compareToIgnoreCase(b.descrizione()))
                .toList();
    }

    /**
     * Crea un party nel mondo indicato (che dev'essere tra i mondi del master)
     * e rende l'utente MASTER del nuovo party.
     */
    @Transactional
    public Integer createParty(CreatePartyRequest request, Utente utente) {
        boolean accessibile = getMieiMondi(utente).stream()
                .anyMatch(m -> Objects.equals(m.id(), request.getMondoId()));
        if (!accessibile)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Mondo non disponibile");

        Mondo mondo = mondoRepository.findById(request.getMondoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mondo non trovato"));

        Party party = new Party();
        party.setNome(request.getNome().trim());
        party.setMondo(mondo);
        Party saved = partyRepository.save(party);

        PermessiParty pp = new PermessiParty();
        pp.setIdUtente(utente);
        pp.setIdParty(saved);
        pp.setRuolo(TipoRuoloParty.MASTER);
        permessiPartyRepository.save(pp);

        return saved.getId();
    }

    /**
     * Crea un personaggio in un party. Il tipo (PG/NPC/BARCA/BANCA/STELLA)
     * diventa la label TIPO_PERSONAGGIO (PG = nessuna label). L'utente che lo
     * crea ne diventa proprietario.
     */
    @Transactional
    public Integer createPersonaggio(CreatePersonaggioRequest request, Utente utente) {
        Party party = partyRepository.findById(request.getPartyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Party non trovato"));
        verificaMembership(party.getId(), utente);

        String tipoLabel = tipoLabelDaTipo(request.getTipo());

        Personaggio pg = new Personaggio();
        pg.setNome(request.getNome().trim());
        pg.setParty(party);
        pg.setLabels(new ArrayList<>());
        if (tipoLabel != null) {
            PersonaggioLabel l = new PersonaggioLabel();
            l.setPersonaggio(pg);
            l.setLabel(LABEL_TIPO_PERSONAGGIO);
            l.setValore(tipoLabel);
            pg.getLabels().add(l);
        }
        Personaggio saved = personaggioRepository.save(pg);

        // proprietario: solo per i PG. Default = chi crea; in alternativa un
        // utente scelto, che dev'essere membro del party.
        if (tipoLabel == null) {
            Utente proprietario = utente;
            if (request.getProprietarioUtenteId() != null
                    && !Objects.equals(request.getProprietarioUtenteId(), utente.getId())) {
                if (!permessiPartyRepository.existsByIdUtente_IdAndIdParty_Id(request.getProprietarioUtenteId(), party.getId()))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Il proprietario dev'essere un membro del party");
                proprietario = utenteRepository.findById(request.getProprietarioUtenteId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utente proprietario non trovato"));
            }
            PermessiPersonaggi perm = new PermessiPersonaggi();
            perm.setIdUtente(proprietario);
            perm.setIdPersonaggio(saved);
            perm.setPermesso(TipoPermessoPersonaggio.PROPRIETARIO);
            permessiPersonaggiRepository.save(perm);
        }

        return saved.getId();
    }

    /**
     * Elimina un party. Consentito solo al master del party e solo se non ha
     * personaggi associati.
     */
    @Transactional
    public void deleteParty(Integer partyId, Utente utente) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Party non trovato"));
        verificaMaster(partyId, utente);

        if (!personaggioRepository.findAllByParty_IdOrderByNomeAsc(partyId).isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Il party non è vuoto");

        permessiPartyRepository.deleteAll(permessiPartyRepository.findAllByIdParty_Id(partyId));
        partyRepository.delete(party);
    }

    /**
     * Membri del party (utenti con permesso) e relativo ruolo.
     */
    public List<MembroPartyDTO> getMembri(Integer partyId, Utente utente) {
        verificaMembership(partyId, utente);
        return permessiPartyRepository.findAllByIdParty_Id(partyId).stream()
                .map(pp -> new MembroPartyDTO(
                        pp.getIdUtente().getId(),
                        pp.getIdUtente().getUsername(),
                        pp.getIdUtente().getName(),
                        pp.getRuolo() != null ? pp.getRuolo().name() : null))
                .sorted((a, b) -> String.valueOf(a.getUsername()).compareToIgnoreCase(String.valueOf(b.getUsername())))
                .toList();
    }

    /**
     * Associa un utente al party (solo il master). Ruolo MASTER o GIOCATORE.
     */
    @Transactional
    public MembroPartyDTO addMembro(Integer partyId, AddMembroRequest request, Utente utente) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Party non trovato"));
        verificaMaster(partyId, utente);

        Utente target = utenteRepository.findByUsernameIgnoreCase(request.getUsername().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utente non trovato"));

        if (permessiPartyRepository.existsByIdUtente_IdAndIdParty_Id(target.getId(), partyId))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'utente fa già parte del party");

        TipoRuoloParty ruolo = "MASTER".equalsIgnoreCase(request.getRuolo())
                ? TipoRuoloParty.MASTER : TipoRuoloParty.GIOCATORE;

        PermessiParty pp = new PermessiParty();
        pp.setIdUtente(target);
        pp.setIdParty(party);
        pp.setRuolo(ruolo);
        permessiPartyRepository.save(pp);

        return new MembroPartyDTO(target.getId(), target.getUsername(), target.getName(), ruolo.name());
    }

    private void verificaMaster(Integer partyId, Utente utente) {
        if (!authzService.isMasterParty(utente, partyId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo il master del party può farlo");
    }

    /**
     * PG = personaggio normale (nessuna label); BARCA usa il valore storico NAVE.
     */
    private static String tipoLabelDaTipo(String tipo) {
        String t = tipo == null ? "" : tipo.trim().toUpperCase(Locale.ROOT);
        return switch (t) {
            case "PG" -> null;
            case "BARCA", "NAVE" -> "NAVE";
            case "NPC" -> "NPC";
            case "BANCA" -> Constants.TIPO_PERSONAGGIO_BANCA;
            case "STELLA" -> "STELLA";
            case "BASE" -> "BASE";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo personaggio non valido: " + tipo);
        };
    }

    private void verificaMembership(Integer partyId, Utente utente) {
        if (!authzService.isMembroParty(utente, partyId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non fai parte di questo party");
    }

    /* =====================================================================
     * Banche (personaggi TIPO_PERSONAGGIO=BANCA, conti = item con label CC)
     * ===================================================================== */

    /**
     * Le banche visibili dal party. Le banche sono condivise (vivono in un
     * party "NPC"): per il party che le ospita si vedono tutti i conti
     * (vista master), per gli altri party solo i conti propri (party e membri).
     */
    public List<BancaDTO> getBanche(Integer partyId, Utente utente) {
        verificaMembership(partyId, utente);

        Set<Integer> idMembri = personaggioRepository.findAllByParty_IdOrderByNomeAsc(partyId).stream()
                .map(Personaggio::getId)
                .collect(Collectors.toSet());

        List<BancaDTO> result = new ArrayList<>();
        for (Personaggio banca : personaggioRepository.findAllBanche()) {
            boolean home = banca.getParty() != null && Objects.equals(banca.getParty().getId(), partyId);

            List<BancaDTO.ContoDTO> conti = new ArrayList<>();
            for (Item itm : (banca.getItems() != null ? banca.getItems() : List.<Item>of())) {
                String cc = itm.getLabel(Constants.LABEL_CC);
                if (cc == null || cc.isBlank()) continue;
                if (!home && !contoVisibileDalParty(cc, partyId, idMembri)) continue;
                conti.add(toContoDTO(itm, cc));
            }
            conti.sort((a, b) -> {
                if (!Objects.equals(a.getTipo(), b.getTipo())) return "PARTY".equals(a.getTipo()) ? -1 : 1;
                return a.getIntestatarioNome().compareToIgnoreCase(b.getIntestatarioNome());
            });
            result.add(new BancaDTO(banca.getId(), banca.getNome(), conti));
        }
        return result;
    }

    /**
     * Tutte le banche viste dal personaggio (per la scheda Soldi): quelle in
     * cui ha un conto lo includono, le altre hanno conti vuoti (per aprirne uno).
     */
    public List<BancaDTO> getContiPersonaggio(Integer personaggioId) {
        String cc = Constants.LABEL_CC_GIOCATORE_PREFIX + personaggioId;
        List<BancaDTO> result = new ArrayList<>();
        for (Personaggio banca : personaggioRepository.findAllBanche()) {
            Item conto = (banca.getItems() != null ? banca.getItems() : List.<Item>of()).stream()
                    .filter(i -> cc.equals(i.getLabel(Constants.LABEL_CC)))
                    .findFirst()
                    .orElse(null);
            result.add(new BancaDTO(
                    banca.getId(),
                    banca.getNome(),
                    conto != null ? List.of(toContoDTO(conto, cc)) : List.of()
            ));
        }
        return result;
    }

    /**
     * Somma di tutti i conti correnti di una banca.
     */
    public PartyDetailDTO.SoldiDTO sommaContiBanca(Personaggio banca) {
        PartyDetailDTO.SoldiDTO totale = new PartyDetailDTO.SoldiDTO(0, 0, 0, 0);
        for (Item itm : (banca.getItems() != null ? banca.getItems() : List.<Item>of())) {
            if (itm.getLabel(Constants.LABEL_CC) == null) continue;
            totale.add(sommaMoneteItem(itm));
        }
        return totale;
    }

    /**
     * Dettaglio banca: conti raggruppati per party. Chi è membro del party
     * della banca (master/NPC) vede tutto, gli altri solo i gruppi dei
     * propri party.
     */
    public BancaDetailDTO getBancaDetail(Integer bancaId, Utente utente) {
        Personaggio banca = personaggioRepository.findPersonaggioById(bancaId);
        if (banca == null || !Constants.TIPO_PERSONAGGIO_BANCA.equals(tipoPersonaggio(banca)))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Banca non trovata");

        Set<Integer> partyUtente = permessiPartyRepository.findAllByIdUtente_Id(utente.getId()).stream()
                .map(p -> p.getIdParty().getId())
                .collect(Collectors.toSet());
        boolean vistaCompleta = authzService.isAdmin(utente)
                || (banca.getParty() != null && partyUtente.contains(banca.getParty().getId()));

        Map<Integer, BancaDetailDTO.GruppoPartyDTO> gruppi = new LinkedHashMap<>();
        PartyDetailDTO.SoldiDTO totale = new PartyDetailDTO.SoldiDTO(0, 0, 0, 0);

        for (Item itm : (banca.getItems() != null ? banca.getItems() : List.<Item>of())) {
            String cc = itm.getLabel(Constants.LABEL_CC);
            if (cc == null || cc.isBlank()) continue;

            BancaDTO.ContoDTO conto = toContoDTO(itm, cc);

            // party di riferimento del conto: per P il party stesso, per G il party del personaggio
            Integer partyId;
            if (cc.startsWith(Constants.LABEL_CC_PARTY_PREFIX)) {
                partyId = conto.getIntestatarioId();
            } else {
                Personaggio pg = personaggioRepository.findPersonaggioById(conto.getIntestatarioId());
                partyId = pg != null && pg.getParty() != null ? pg.getParty().getId() : null;
            }

            if (!vistaCompleta && (partyId == null || !partyUtente.contains(partyId))) continue;

            Integer key = partyId != null ? partyId : -1;
            BancaDetailDTO.GruppoPartyDTO gruppo = gruppi.computeIfAbsent(key, k -> new BancaDetailDTO.GruppoPartyDTO(
                    partyId,
                    partyId != null
                            ? partyRepository.findById(partyId).map(Party::getNome).orElse("Party " + partyId)
                            : "Senza party",
                    new PartyDetailDTO.SoldiDTO(0, 0, 0, 0),
                    new ArrayList<>()
            ));
            gruppo.getConti().add(conto);
            gruppo.getTotale().add(conto.getSoldi());
            totale.add(conto.getSoldi());
        }

        List<BancaDetailDTO.GruppoPartyDTO> lista = new ArrayList<>(gruppi.values());
        lista.sort((a, b) -> a.getPartyNome().compareToIgnoreCase(b.getPartyNome()));
        for (BancaDetailDTO.GruppoPartyDTO g : lista) {
            g.getConti().sort((a, b) -> {
                if (!Objects.equals(a.getTipo(), b.getTipo())) return "PARTY".equals(a.getTipo()) ? -1 : 1;
                return a.getIntestatarioNome().compareToIgnoreCase(b.getIntestatarioNome());
            });
        }

        return new BancaDetailDTO(banca.getId(), banca.getNome(), totale, lista);
    }

    private static boolean contoVisibileDalParty(String cc, Integer partyId, Set<Integer> idMembri) {
        int id = parseIntSafe(cc.substring(1));
        if (cc.startsWith(Constants.LABEL_CC_PARTY_PREFIX)) return Objects.equals(id, partyId);
        return idMembri.contains(id);
    }

    /**
     * Apre un nuovo conto (item con label CC) in una banca. Permesso: membro
     * del party della banca (master/NPC) oppure del party dell'intestatario.
     */
    @Transactional
    public BancaDTO.ContoDTO apriConto(Integer bancaId, String cc, Utente utente) {
        Personaggio banca = personaggioRepository.findPersonaggioById(bancaId);
        if (banca == null || !Constants.TIPO_PERSONAGGIO_BANCA.equals(tipoPersonaggio(banca)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Il personaggio non è una banca");

        String ccNorm = cc == null ? "" : cc.trim().toUpperCase(Locale.ROOT);
        if (!ccNorm.matches("^[GP]\\d+$"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato CC non valido (G<id> o P<id>)");
        int intestatarioId = Integer.parseInt(ccNorm.substring(1));

        // intestatario valido + nome
        String intestatario;
        Integer partyIntestatario;
        if (ccNorm.startsWith(Constants.LABEL_CC_GIOCATORE_PREFIX)) {
            Personaggio pg = personaggioRepository.findPersonaggioById(intestatarioId);
            if (pg == null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Personaggio intestatario non trovato");
            intestatario = pg.getNome();
            partyIntestatario = pg.getParty() != null ? pg.getParty().getId() : null;
        } else {
            Party party = partyRepository.findById(intestatarioId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Party intestatario non trovato"));
            intestatario = party.getNome();
            partyIntestatario = party.getId();
        }

        verificaPermessoConto(banca, partyIntestatario, utente);

        // conto già esistente in questa banca?
        boolean esiste = (banca.getItems() != null ? banca.getItems() : List.<Item>of()).stream()
                .anyMatch(i -> ccNorm.equals(i.getLabel(Constants.LABEL_CC)));
        if (esiste) throw new ResponseStatusException(HttpStatus.CONFLICT, "Conto già esistente in questa banca");

        Item conto = new Item();
        conto.setNome("Conto " + intestatario);
        conto.setTipo(TipoItem.ALTRO);
        conto.setPersonaggio(banca);
        conto.setLabels(new ArrayList<>());
        if (banca.getParty() != null && banca.getParty().getMondo() != null) {
            conto.setMondo(banca.getParty().getMondo());
            conto.setSistema(banca.getParty().getMondo().getSistema());
        }
        ItemLabel lbl = new ItemLabel();
        lbl.setItem(conto);
        lbl.setLabel(Constants.LABEL_CC);
        lbl.setValore(ccNorm);
        conto.getLabels().add(lbl);
        Item saved = itemRepository.save(conto);

        return toContoDTO(saved, ccNorm);
    }

    /**
     * Imposta le monete di un conto banca. Permesso: membro del party della
     * banca (master/NPC) oppure del party dell'intestatario.
     */
    @Transactional
    public PartyDetailDTO.SoldiDTO updateConto(Integer itemId, PartyDetailDTO.SoldiDTO target, Utente utente) {
        Item conto = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conto non trovato"));
        String cc = conto.getLabel(Constants.LABEL_CC);
        if (cc == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'item non è un conto banca");
        Personaggio banca = conto.getPersonaggio();
        if (banca == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Conto non associato a una banca");

        Integer partyIntestatario = null;
        int intestatarioId = parseIntSafe(cc.substring(1));
        if (cc.startsWith(Constants.LABEL_CC_GIOCATORE_PREFIX)) {
            Personaggio pg = personaggioRepository.findPersonaggioById(intestatarioId);
            if (pg != null && pg.getParty() != null) partyIntestatario = pg.getParty().getId();
        } else {
            partyIntestatario = intestatarioId;
        }
        verificaPermessoConto(banca, partyIntestatario, utente);

        setModificatoreMoneta(conto, "MR", Math.max(0, target.getMr()));
        setModificatoreMoneta(conto, "MA", Math.max(0, target.getMa()));
        setModificatoreMoneta(conto, "MO", Math.max(0, target.getMo()));
        setModificatoreMoneta(conto, "MP", Math.max(0, target.getMp()));
        return target;
    }

    private void verificaPermessoConto(Personaggio banca, Integer partyIntestatario, Utente utente) {
        if (authzService.isAdmin(utente)) return;
        Set<Integer> partyUtente = permessiPartyRepository.findAllByIdUtente_Id(utente.getId()).stream()
                .map(p -> p.getIdParty().getId())
                .collect(Collectors.toSet());
        boolean membroBanca = banca.getParty() != null && partyUtente.contains(banca.getParty().getId());
        boolean membroIntestatario = partyIntestatario != null && partyUtente.contains(partyIntestatario);
        if (!membroBanca && !membroIntestatario)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non hai accesso a questo conto");
    }

    private BancaDTO.ContoDTO toContoDTO(Item itm, String cc) {
        boolean isParty = cc.startsWith(Constants.LABEL_CC_PARTY_PREFIX);
        int intestatarioId = parseIntSafe(cc.substring(1));
        String nome;
        if (isParty) {
            nome = partyRepository.findById(intestatarioId).map(Party::getNome)
                    .orElse("Party " + intestatarioId);
        } else {
            Personaggio pg = personaggioRepository.findPersonaggioById(intestatarioId);
            nome = pg != null ? pg.getNome() : "Personaggio " + intestatarioId;
        }
        return new BancaDTO.ContoDTO(
                itm.getId(),
                cc,
                isParty ? "PARTY" : "GIOCATORE",
                intestatarioId,
                nome,
                sommaMoneteItem(itm)
        );
    }

    private static PartyDetailDTO.SoldiDTO sommaMoneteItem(Item itm) {
        List<Modificatore> mods = (itm.getModificatori() != null ? itm.getModificatori() : List.<Modificatore>of()).stream()
                .filter(m -> STAT_MONETE.contains(m.getStat().getId()))
                .toList();
        PartyDetailDTO.SoldiDTO soldi = new PartyDetailDTO.SoldiDTO(0, 0, 0, 0);
        for (Modificatore m : mods) {
            long valore = parseLong(m.getValore());
            switch (m.getStat().getId()) {
                case "MR" -> soldi.setMr(soldi.getMr() + valore);
                case "MA" -> soldi.setMa(soldi.getMa() + valore);
                case "MO" -> soldi.setMo(soldi.getMo() + valore);
                case "MP" -> soldi.setMp(soldi.getMp() + valore);
            }
        }
        return soldi;
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Peso delle monete in kg, per taglio (per ora tutte 10 grammi).
     */
    private static final double PESO_MR_KG = 0.01;
    private static final double PESO_MA_KG = 0.01;
    private static final double PESO_MO_KG = 0.01;
    private static final double PESO_MP_KG = 0.01;

    public static double calcolaPesoMonete(PartyDetailDTO.SoldiDTO soldi) {
        return soldi.getMr() * PESO_MR_KG
                + soldi.getMa() * PESO_MA_KG
                + soldi.getMo() * PESO_MO_KG
                + soldi.getMp() * PESO_MP_KG;
    }

    /**
     * Peso trasportato (kg): somma di (PESO x QTA) sugli item del personaggio
     * (diretti e collegati via FromCompendio), più la sua personaggio_label PESO,
     * più il peso delle monete. Item senza peso valgono 0, QTA assente vale 1.
     */
    public double calcolaPeso(Personaggio pg) {
        // Raccogli tutte le label rilevanti per il calcolo del peso
        Map<Integer, Map<String, String>> labelsPerItem = new HashMap<>();
        for (Object[] row : itemLabelRepository.findLabelValuesByPersonaggio(
                List.of(Constants.LABEL_PESO, Constants.LABEL_QTA, Constants.LABEL_CAPIENZA,
                        Constants.ITEM_LABEL_DISABILITATO,
                        Constants.LABEL_INCLUDI_ARMI_ABILITATE,
                        Constants.LABEL_INCLUDI_OGGETTI_ABILITATI,
                        Constants.LABEL_INCLUDI_CONSUMABILI_ABILITATI), pg.getId())) {
            labelsPerItem.computeIfAbsent((Integer) row[0], k -> new HashMap<>())
                    .put((String) row[1], (String) row[2]);
        }

        // Raccogli il tipo di ogni item
        Map<Integer, TipoItem> tipoPerItem = new HashMap<>();
        for (Object[] row : itemRepository.findIdAndTipoByPersonaggioId(pg.getId())) {
            tipoPerItem.put((Integer) row[0], (TipoItem) row[1]);
        }

        record ContainerInfo(double pesoMassimo, double capienza,
                             boolean includiArmi, boolean includiOggetti, boolean includiConsumabili) {}
        List<ContainerInfo> containers = new ArrayList<>();
        double containableWeight = 0;      // item disabilitati → entrano sempre
        double armeAbilitateWeight = 0;    // ARMA abilitate → entrano solo con flag
        double oggettiAbilitatiWeight = 0; // OGGETTO abilitati → entrano solo con flag
        double consumabiliAbilitatiWeight = 0; // CONSUMABILE abilitati → entrano solo con flag
        double equippedWeight = 0;         // tutto il resto abilitato → peso diretto

        for (Map.Entry<Integer, Map<String, String>> entry : labelsPerItem.entrySet()) {
            Integer id = entry.getKey();
            Map<String, String> labels = entry.getValue();
            TipoItem tipo = tipoPerItem.get(id);
            String pesoStr = labels.get(Constants.LABEL_PESO);
            if (pesoStr == null) continue;
            double itemPeso = parsePeso(pesoStr) * parseQta(labels.get(Constants.LABEL_QTA));
            boolean isDisabled = Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE
                    .equals(labels.get(Constants.ITEM_LABEL_DISABILITATO));

            if (TipoItem.CONTENITORE.equals(tipo)) {
                double capienza = parsePeso(labels.get(Constants.LABEL_CAPIENZA));
                boolean inclArmi = "1".equals(labels.get(Constants.LABEL_INCLUDI_ARMI_ABILITATE));
                boolean inclOgg  = "1".equals(labels.get(Constants.LABEL_INCLUDI_OGGETTI_ABILITATI));
                boolean inclCons = "1".equals(labels.get(Constants.LABEL_INCLUDI_CONSUMABILI_ABILITATI));
                containers.add(new ContainerInfo(itemPeso, capienza, inclArmi, inclOgg, inclCons));
            } else if (isDisabled) {
                containableWeight += itemPeso;
            } else if (TipoItem.ARMA.equals(tipo)) {
                armeAbilitateWeight += itemPeso;
            } else if (TipoItem.OGGETTO.equals(tipo)) {
                oggettiAbilitatiWeight += itemPeso;
            } else if (TipoItem.CONSUMABILE.equals(tipo)) {
                consumabiliAbilitatiWeight += itemPeso;
            } else {
                equippedWeight += itemPeso;
            }
        }

        // Peso fisso del personaggio
        if (pg.getLabels() != null) {
            equippedWeight += pg.getLabels().stream()
                    .filter(l -> Constants.LABEL_PESO.equals(l.getLabel()))
                    .mapToDouble(l -> parsePeso(l.getValore()))
                    .sum();
        }

        // Monete: vanno nei contenitori come i disabilitati
        containableWeight += calcolaPesoMonete(calcolaSoldi(pg.getId()));

        // Riempi i contenitori dal più capiente al meno capiente
        containers.sort((a, b) -> Double.compare(b.capienza(), a.capienza()));
        double remContainable = containableWeight;
        double remArme        = armeAbilitateWeight;
        double remOggetti     = oggettiAbilitatiWeight;
        double remConsumabili = consumabiliAbilitatiWeight;
        double containerWeight = 0;

        for (ContainerInfo c : containers) {
            if (c.capienza() <= 0) { containerWeight += c.pesoMassimo(); continue; }
            double spaceLeft = c.capienza();
            // 1. item disabilitati
            double f = Math.min(spaceLeft, remContainable); remContainable -= f; spaceLeft -= f;
            // 2. armi abilitate (se flag)
            if (c.includiArmi() && spaceLeft > 0) {
                f = Math.min(spaceLeft, remArme); remArme -= f; spaceLeft -= f;
            }
            // 3. oggetti abilitati (se flag)
            if (c.includiOggetti() && spaceLeft > 0) {
                f = Math.min(spaceLeft, remOggetti); remOggetti -= f; spaceLeft -= f;
            }
            // 4. consumabili abilitati (se flag)
            if (c.includiConsumabili() && spaceLeft > 0) {
                f = Math.min(spaceLeft, remConsumabili); remConsumabili -= f; spaceLeft -= f;
            }
            double filled = c.capienza() - spaceLeft;
            containerWeight += c.pesoMassimo() * (filled / c.capienza());
        }

        double total = equippedWeight + containerWeight
                + remContainable + remArme + remOggetti + remConsumabili;
        return Math.round(total * 100) / 100.0;
    }

    private static int parseQta(String s) {
        if (s == null) return 1;
        try {
            return Math.max(0, Integer.parseInt(s.trim()));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static double parsePeso(String s) {
        if (s == null) return 0;
        try {
            return Double.parseDouble(s.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String tipoPersonaggio(Personaggio pg) {
        if (pg.getLabels() == null) return null;
        return pg.getLabels().stream()
                .filter(l -> LABEL_TIPO_PERSONAGGIO.equals(l.getLabel()))
                .map(PersonaggioLabel::getValore)
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElse(null);
    }

    /* =====================================================================
     * Gruppi di personaggi
     * ===================================================================== */

    /** Valore di una personaggio_label, o null se assente. */
    private static String personaggioLabel(Personaggio pg, String key) {
        if (pg.getLabels() == null) return null;
        return pg.getLabels().stream()
                .filter(l -> key.equals(l.getLabel()))
                .map(PersonaggioLabel::getValore)
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElse(null);
    }

    /** Imposta/rimuove (value=null) una personaggio_label. Da salvare con save(pg). */
    private static void setPersonaggioLabel(Personaggio pg, String key, String value) {
        if (pg.getLabels() == null) pg.setLabels(new ArrayList<>());
        pg.getLabels().removeIf(l -> key.equals(l.getLabel()));
        if (value != null) {
            PersonaggioLabel l = new PersonaggioLabel();
            l.setPersonaggio(pg);
            l.setLabel(key);
            l.setValore(value);
            pg.getLabels().add(l);
        }
    }

    public List<GruppoDTO> getGruppi(Integer partyId, Utente utente) {
        verificaMembership(partyId, utente);
        List<Personaggio> membri = personaggioRepository.findAllByParty_IdOrderByNomeAsc(partyId);
        return gruppoRepository.findAllByParty_IdOrderByNomeAsc(partyId).stream()
                .map(g -> {
                    List<Integer> membriIds = new ArrayList<>();
                    Integer capo = null;
                    for (Personaggio pg : membri) {
                        if (String.valueOf(g.getId()).equals(personaggioLabel(pg, Constants.LABEL_GRUPPO))) {
                            membriIds.add(pg.getId());
                            if ("1".equals(personaggioLabel(pg, Constants.LABEL_CAPOGRUPPO))) capo = pg.getId();
                        }
                    }
                    return new GruppoDTO(g.getId(), g.getNome(), membriIds, capo);
                })
                .toList();
    }

    @Transactional
    public GruppoDTO createGruppo(Integer partyId, String nome, Utente utente) {
        Party party = partyRepository.findById(partyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Party non trovato"));
        verificaMembership(partyId, utente);
        if (nome == null || nome.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome gruppo obbligatorio");
        Gruppo g = new Gruppo();
        g.setParty(party);
        g.setNome(nome.trim());
        Gruppo saved = gruppoRepository.save(g);
        return new GruppoDTO(saved.getId(), saved.getNome(), new ArrayList<>(), null);
    }

    /** Aggiorna nome, membri e capogruppo di un gruppo (stato completo). */
    @Transactional
    public GruppoDTO saveGruppo(Integer gruppoId, SaveGruppoRequest req, Utente utente) {
        Gruppo g = gruppoRepository.findById(gruppoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gruppo non trovato"));
        Integer partyId = g.getParty().getId();
        verificaMembership(partyId, utente);

        if (req.getNome() != null && !req.getNome().isBlank()) {
            g.setNome(req.getNome().trim());
            gruppoRepository.save(g);
        }

        Set<Integer> desiderati = new HashSet<>(req.getMembriIds() != null ? req.getMembriIds() : List.of());
        // le banche non si assegnano ai gruppi
        Integer capo = req.getCapogruppoId();

        for (Personaggio pg : personaggioRepository.findAllByParty_IdOrderByNomeAsc(partyId)) {
            boolean isBanca = Constants.TIPO_PERSONAGGIO_BANCA.equals(tipoPersonaggio(pg));
            boolean inQuestoGruppo = String.valueOf(gruppoId).equals(personaggioLabel(pg, Constants.LABEL_GRUPPO));
            boolean deveStare = !isBanca && desiderati.contains(pg.getId());
            if (deveStare) {
                setPersonaggioLabel(pg, Constants.LABEL_GRUPPO, String.valueOf(gruppoId));
                setPersonaggioLabel(pg, Constants.LABEL_CAPOGRUPPO, pg.getId().equals(capo) ? "1" : null);
                personaggioRepository.save(pg);
            } else if (inQuestoGruppo) {
                setPersonaggioLabel(pg, Constants.LABEL_GRUPPO, null);
                setPersonaggioLabel(pg, Constants.LABEL_CAPOGRUPPO, null);
                personaggioRepository.save(pg);
            }
        }
        return new GruppoDTO(g.getId(), g.getNome(), new ArrayList<>(desiderati), capo);
    }

    @Transactional
    public void deleteGruppo(Integer gruppoId, Utente utente) {
        Gruppo g = gruppoRepository.findById(gruppoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gruppo non trovato"));
        Integer partyId = g.getParty().getId();
        verificaMembership(partyId, utente);
        // pulisci le label di appartenenza dei membri
        for (Personaggio pg : personaggioRepository.findAllByParty_IdOrderByNomeAsc(partyId)) {
            if (String.valueOf(gruppoId).equals(personaggioLabel(pg, Constants.LABEL_GRUPPO))) {
                setPersonaggioLabel(pg, Constants.LABEL_GRUPPO, null);
                setPersonaggioLabel(pg, Constants.LABEL_CAPOGRUPPO, null);
                personaggioRepository.save(pg);
            }
        }
        gruppoRepository.delete(g);
    }

    /**
     * Soldi personali: modificatori sulle stat moneta degli item del
     * personaggio, esclusi i conti banca (label CC), ignorando gli item
     * disabilitati e i valori non numerici.
     */
    public PartyDetailDTO.SoldiDTO calcolaSoldi(Integer personaggioId) {
        return sommaMonete(modificatoreRepository.findMonetePersonali(personaggioId, STAT_MONETE));
    }

    /**
     * Soldi su un conto banca (label CC = G&lt;id&gt; | P&lt;id&gt;).
     */
    public PartyDetailDTO.SoldiDTO calcolaSoldiConto(String cc) {
        return sommaMonete(modificatoreRepository.findMoneteConto(cc, STAT_MONETE));
    }

    private static PartyDetailDTO.SoldiDTO sommaMonete(List<Modificatore> mods) {
        PartyDetailDTO.SoldiDTO soldi = new PartyDetailDTO.SoldiDTO(0, 0, 0, 0);
        for (Modificatore m : mods) {
            if (m.getItem() != null && Boolean.TRUE.equals(m.getItem().isDisabled())) continue;
            long valore = parseLong(m.getValore());
            switch (m.getStat().getId()) {
                case "MR" -> soldi.setMr(soldi.getMr() + valore);
                case "MA" -> soldi.setMa(soldi.getMa() + valore);
                case "MO" -> soldi.setMo(soldi.getMo() + valore);
                case "MP" -> soldi.setMp(soldi.getMp() + valore);
            }
        }
        return soldi;
    }

    /**
     * Imposta i soldi totali del personaggio. La differenza rispetto a quanto
     * proviene dagli altri item viene scritta sull'item "Borsellino" del
     * personaggio (creato se non esiste), un modificatore per moneta.
     */
    @Transactional
    public PartyDetailDTO.SoldiDTO updateSoldi(Integer personaggioId, PartyDetailDTO.SoldiDTO target) {
        Personaggio pg = personaggioRepository.findPersonaggioById(personaggioId);
        if (pg == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Personaggio non trovato");

        Item borsellino = itemRepository.findItemByNomeAndPersonaggio_Id(NOME_ITEM_BORSELLINO, personaggioId);
        if (borsellino == null) {
            borsellino = new Item();
            borsellino.setNome(NOME_ITEM_BORSELLINO);
            borsellino.setTipo(TipoItem.OGGETTO);
            borsellino.setPersonaggio(pg);
            borsellino.setLabels(new ArrayList<>());
            borsellino.setDescrizione("Monete del personaggio");
            borsellino = itemRepository.save(borsellino);
        }

        PartyDetailDTO.SoldiDTO totale = calcolaSoldi(personaggioId);
        PartyDetailDTO.SoldiDTO bors = new PartyDetailDTO.SoldiDTO(
                valoreModificatore(borsellino, "MR"),
                valoreModificatore(borsellino, "MA"),
                valoreModificatore(borsellino, "MO"),
                valoreModificatore(borsellino, "MP")
        );

        // contributo degli altri item = totale - borsellino attuale
        setModificatoreMoneta(borsellino, "MR", target.getMr() - (totale.getMr() - bors.getMr()));
        setModificatoreMoneta(borsellino, "MA", target.getMa() - (totale.getMa() - bors.getMa()));
        setModificatoreMoneta(borsellino, "MO", target.getMo() - (totale.getMo() - bors.getMo()));
        setModificatoreMoneta(borsellino, "MP", target.getMp() - (totale.getMp() - bors.getMp()));

        return target;
    }

    private long valoreModificatore(Item item, String statId) {
        Modificatore m = item.getModificatore(statId);
        return m == null ? 0 : parseLong(m.getValore());
    }

    private void setModificatoreMoneta(Item borsellino, String statId, long valore) {
        Modificatore m = borsellino.getModificatore(statId);
        if (m == null) {
            Stat stat = em.find(Stat.class, statId);
            if (stat == null) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Stat moneta mancante: " + statId + " (eseguire la migrazione)");
            m = new Modificatore();
            m.setItem(borsellino);
            m.setStat(stat);
            m.setTipo(TipoModificatore.VALORE);
        }
        m.setValore(String.valueOf(valore));
        modificatoreRepository.save(m);
    }

    private static long parseLong(String s) {
        if (s == null) return 0;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
