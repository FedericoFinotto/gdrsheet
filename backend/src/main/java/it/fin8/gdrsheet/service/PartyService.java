package it.fin8.gdrsheet.service;

import it.fin8.gdrsheet.config.Constants;
import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.def.TipoModificatore;
import it.fin8.gdrsheet.def.TipoPermessoPersonaggio;
import it.fin8.gdrsheet.dto.GiveItemRequest;
import it.fin8.gdrsheet.dto.PartyDetailDTO;
import it.fin8.gdrsheet.dto.PartyItemDTO;
import it.fin8.gdrsheet.entity.*;
import it.fin8.gdrsheet.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
    private final EntityManager em;

    public PartyService(PermessiPartyRepository permessiPartyRepository,
                        PermessiPersonaggiRepository permessiPersonaggiRepository,
                        PersonaggioRepository personaggioRepository,
                        ModificatoreRepository modificatoreRepository,
                        ItemRepository itemRepository,
                        ItemLabelRepository itemLabelRepository,
                        CollegamentoRepository collegamentoRepository,
                        ItemService itemService,
                        EntityManager em) {
        this.permessiPartyRepository = permessiPartyRepository;
        this.permessiPersonaggiRepository = permessiPersonaggiRepository;
        this.personaggioRepository = personaggioRepository;
        this.modificatoreRepository = modificatoreRepository;
        this.itemRepository = itemRepository;
        this.itemLabelRepository = itemLabelRepository;
        this.collegamentoRepository = collegamentoRepository;
        this.itemService = itemService;
        this.em = em;
    }

    public PartyDetailDTO getPartyDetail(Integer partyId, Utente utente) {
        PermessiParty permesso = permessiPartyRepository.findAllByIdUtente_Id(utente.getId()).stream()
                .filter(p -> Objects.equals(p.getIdParty().getId(), partyId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Non fai parte di questo party"));

        List<Personaggio> membri = personaggioRepository.findAllByParty_IdOrderByNomeAsc(partyId);

        // personaggi di cui l'utente corrente è proprietario
        Set<Integer> posseduti = permessiPersonaggiRepository.findAllByIdUtente_Id(utente.getId()).stream()
                .filter(p -> TipoPermessoPersonaggio.PROPRIETARIO.equals(p.getPermesso()))
                .map(p -> p.getIdPersonaggio().getId())
                .collect(Collectors.toSet());

        List<PartyDetailDTO.PersonaggioSoldiDTO> personaggi = new ArrayList<>();
        PartyDetailDTO.SoldiDTO somma = new PartyDetailDTO.SoldiDTO(0, 0, 0, 0);
        double pesoTotale = 0;

        for (Personaggio pg : membri) {
            PartyDetailDTO.SoldiDTO soldi = calcolaSoldi(pg.getId());
            somma.add(soldi);
            double peso = calcolaPeso(pg);
            pesoTotale += peso;
            personaggi.add(new PartyDetailDTO.PersonaggioSoldiDTO(
                    pg.getId(),
                    pg.getNome(),
                    soldi,
                    tipoPersonaggio(pg),
                    posseduti.contains(pg.getId()),
                    peso
            ));
        }

        return new PartyDetailDTO(
                permesso.getIdParty().getId(),
                permesso.getIdParty().getNome(),
                permesso.getRuolo() != null ? permesso.getRuolo().name() : null,
                personaggi,
                somma,
                pesoTotale
        );
    }

    /**
     * Tutti gli item di inventario dei membri del party, con peso e proprietario.
     */
    public List<PartyItemDTO> getPartyItems(Integer partyId, Utente utente) {
        verificaMembership(partyId, utente);

        List<Personaggio> membri = personaggioRepository.findAllByParty_IdOrderByNomeAsc(partyId);
        List<PartyItemDTO> result = new ArrayList<>();

        for (Personaggio pg : membri) {
            Item fromCompendio = itemRepository.findItemByNomeAndPersonaggio_Id(Constants.ITEM_FROM_COMPENDIO, pg.getId());
            if (fromCompendio == null) continue;
            for (Collegamento link : collegamentoRepository.findAllByItemSource_Id(fromCompendio.getId())) {
                Item itm = link.getItemTarget();
                if (!TIPI_INVENTARIO.contains(itm.getTipo())) continue;
                result.add(new PartyItemDTO(
                        itm.getId(),
                        itm.getNome(),
                        itm.getTipo().name(),
                        parsePeso(itm.getLabel(Constants.LABEL_PESO)),
                        pg.getId(),
                        pg.getNome(),
                        Boolean.TRUE.equals(itm.isDisabled()) || Boolean.TRUE.equals(link.isDisabled())
                ));
            }
        }

        result.sort((a, b) -> a.getNome().compareToIgnoreCase(b.getNome()));
        return result;
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

    private void verificaMembership(Integer partyId, Utente utente) {
        boolean membro = permessiPartyRepository.findAllByIdUtente_Id(utente.getId()).stream()
                .anyMatch(p -> Objects.equals(p.getIdParty().getId(), partyId));
        if (!membro) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non fai parte di questo party");
    }

    /**
     * Peso trasportato (kg): somma delle label PESO degli item del personaggio
     * (diretti e collegati via FromCompendio) più la sua personaggio_label PESO.
     * Item senza peso valgono 0.
     */
    public double calcolaPeso(Personaggio pg) {
        double peso = itemLabelRepository.findValoriLabelByPersonaggio(Constants.LABEL_PESO, pg.getId()).stream()
                .mapToDouble(PartyService::parsePeso)
                .sum();
        if (pg.getLabels() != null) {
            peso += pg.getLabels().stream()
                    .filter(l -> Constants.LABEL_PESO.equals(l.getLabel()))
                    .mapToDouble(l -> parsePeso(l.getValore()))
                    .sum();
        }
        return Math.round(peso * 100) / 100.0;
    }

    private static double parsePeso(String s) {
        if (s == null) return 0;
        try {
            return Double.parseDouble(s.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String tipoPersonaggio(Personaggio pg) {
        if (pg.getLabels() == null) return null;
        return pg.getLabels().stream()
                .filter(l -> LABEL_TIPO_PERSONAGGIO.equals(l.getLabel()))
                .map(PersonaggioLabel::getValore)
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElse(null);
    }

    /**
     * Somma i modificatori sulle stat moneta (MR/MA/MO/MP) degli item del
     * personaggio, ignorando gli item disabilitati e i valori non numerici.
     */
    public PartyDetailDTO.SoldiDTO calcolaSoldi(Integer personaggioId) {
        PartyDetailDTO.SoldiDTO soldi = new PartyDetailDTO.SoldiDTO(0, 0, 0, 0);
        List<Modificatore> mods = modificatoreRepository.findAllByPersonaggioIdAndStatIds(personaggioId, STAT_MONETE);
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
