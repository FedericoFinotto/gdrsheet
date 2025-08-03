package it.fin8.grdsheet.service;

import it.fin8.grdsheet.def.TipoItem;
import it.fin8.grdsheet.def.TipoModificatore;
import it.fin8.grdsheet.def.TipoStat;
import it.fin8.grdsheet.dto.*;
import it.fin8.grdsheet.entity.*;
import it.fin8.grdsheet.mapper.ItemMapper;
import it.fin8.grdsheet.mapper.ModificatoreMapper;
import it.fin8.grdsheet.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class PersonaggioService {

    @Autowired
    private PersonaggioRepository personaggioRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ModificatoreRepository modificatoreRepository;

    @Autowired
    private StatValueRepository statValueRepository;

    @Autowired
    private ItemLabelRepository itemLabelRepository;

    @Autowired
    private ModificatoreMapper modificatoreMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private ModificatoriService modificatoriService;

    /**
     * Flattens iteratively the item hierarchy into a list.
     */
    private List<Item> flattenItems(Collection<Item> rootItems) {
        List<Item> result = new ArrayList<>();
        Deque<Item> stack = new ArrayDeque<>(rootItems);
        while (!stack.isEmpty()) {
            Item cur = stack.pop();
            if (result.add(cur) && cur.getChild() != null) {
                for (Collegamento col : cur.getChild()) {
                    stack.push(col.getItemTarget());
                }
            }
        }
        return result;
    }

    public ItemsDTO getAllPersonaggioItemsDTOByIdPersonaggio(Integer id) {
        ItemsDTO itemsDTO = new ItemsDTO();
//        'ABILITA', 'TALENTO', 'OGGETTO', 'CONSUMABILE', 'ARMA', 'MUNIZIONE', 'EQUIPAGGIAMENTO',
//        'PERSONAGGIO', 'CLASSE', 'RAZZA', 'ATTACCO', 'ALTRO', 'LIVELLO', 'MALEDIZIONE', 'INCANTESIMO'
        getAllPersonaggioItemsByIdPersonaggio(id).forEach(itm -> {
                    if (TipoItem.ABILITA.equals(itm.getTipo())) {
                        itemsDTO.getAbilita().add(itemMapper.toDTO(itm));
                    }
                    if (TipoItem.TALENTO.equals(itm.getTipo())) {
                        itemsDTO.getTalenti().add(itemMapper.toDTO(itm));
                    }
                    if (TipoItem.OGGETTO.equals(itm.getTipo())) {
                        itemsDTO.getOggetti().add(itemMapper.toDTO(itm));
                    }
                    if (TipoItem.CONSUMABILE.equals(itm.getTipo())) {
                        itemsDTO.getConsumabili().add(itemMapper.toDTO(itm));
                    }
                    if (TipoItem.ARMA.equals(itm.getTipo())) {
                        itemsDTO.getArmi().add(itemMapper.toDTO(itm));
                    }
                    if (TipoItem.MUNIZIONE.equals(itm.getTipo())) {
                        itemsDTO.getMunizioni().add(itemMapper.toDTO(itm));
                    }
                    if (TipoItem.EQUIPAGGIAMENTO.equals(itm.getTipo())) {
                        itemsDTO.getEquipaggiamento().add(itemMapper.toDTO(itm));
                    }
                    if (TipoItem.CLASSE.equals(itm.getTipo())) {
                        itemsDTO.getClassi().add(itemMapper.toDTO(itm));
                    }
                    if (TipoItem.RAZZA.equals(itm.getTipo())) {
                        itemsDTO.getRazze().add(itemMapper.toDTO(itm));
                    }
                    if (TipoItem.ATTACCO.equals(itm.getTipo())) {
                        itemsDTO.getAttacchi().add(itemMapper.toAttaccoDTO(itm));
                    }
                    if (TipoItem.LIVELLO.equals(itm.getTipo())) {
                        itemsDTO.getLivelli().add(itemMapper.toDTO(itm));
                    }
                    if (TipoItem.MALEDIZIONE.equals(itm.getTipo())) {
                        itemsDTO.getMaledizioni().add(itemMapper.toDTO(itm));
                    }
                    if (TipoItem.INCANTESIMO.equals(itm.getTipo())) {
                        itemsDTO.getIncantesimi().add(itemMapper.toIncantesimoDTO(itm));
                    }
                }
        );
        return itemsDTO;
    }

    public List<Item> getAllPersonaggioItemsByIdPersonaggio(Integer idPersonaggio) {

        // Fetch Items principali con un solo join-fetch su child
        List<Item> initialRoots = itemRepository.findAllByPersonaggioIdWithChild(idPersonaggio);

        // Determina le classi e livelli dai soli rootItems
        Map<Item, Long> classLevels = initialRoots.stream()
                .filter(i -> TipoItem.LIVELLO.equals(i.getTipo()))
                .map(liv -> liv.getChild().stream()
                        .filter(c -> TipoItem.CLASSE.equals(c.getItemTarget().getTipo()))
                        .map(Collegamento::getItemTarget)
                        .findFirst().orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(c -> c, Collectors.counting()));

        // Raccogli root items aggiuntive dagli avanzamenti di classe
        List<Item> advanceRoots = new ArrayList<>();
        for (Map.Entry<Item, Long> entry : classLevels.entrySet()) {
            Item classe = entry.getKey();
            long livello = entry.getValue();
            List<ItemLivelloDTO> incantesimi = new ArrayList<>();
            ItemLabel spellDiClasse = classe.getLabels().stream().filter(x -> x.getLabel().equals("SPELL")).findFirst().orElse(null);
            if (spellDiClasse != null) {
                ItemLabel preparedSpell = itemLabelRepository.findByItemPersonaggioIdAndItemNome(idPersonaggio, "PreparedSpell").stream().filter(x -> x.getLabel().equals(spellDiClasse.getValore())).findFirst().orElse(null);
                if (preparedSpell != null) {
                    List<Integer> incantesimiIds = Stream.of(preparedSpell.getValore().split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(Integer::parseInt)
                            .toList();
                    incantesimi = itemRepository.findIncantesimiWithLivelloByLabelAndIds(spellDiClasse.getValore(), incantesimiIds);
                    if (!incantesimi.isEmpty()) {
                        incantesimi.forEach(item -> {
                            item.getItem().getLabels().add(new ItemLabel(null, null, "CLIVELLO", item.getLivello()));
                            item.getItem().getLabels().add(new ItemLabel(null, null, "CSLASSE", spellDiClasse.getValore()));
                        });
                        advanceRoots.addAll(incantesimi.stream().map(ItemLivelloDTO::getItem).toList());
                    }
                }


            }

            if (classe.getAvanzamento() != null) {
                for (Avanzamento av : classe.getAvanzamento()) {
                    if (av.getLivello() <= livello) {
                        advanceRoots.add(av.getItemTarget());
                    }
                }
            }

        }

        // Esegui un unico flatten di tutti i root (main + advance)
        List<Item> allItems = flattenItems(
                Stream.concat(initialRoots.stream(), advanceRoots.stream())
                        .collect(Collectors.toList())
        );

        return allItems;
    }

    /**
     * Ottiene i dati del personaggio, includendo items da avanzamenti di classe in base al livello.
     * Unifica i flatten in un solo passaggio per migliorare le prestazioni.
     */
    public DatiPersonaggioDTO getDatiPersonaggio(Integer id) {
        // 1) Carica Personaggio base
        Personaggio p = personaggioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Personaggio non trovato"));

        List<Item> allItems = getAllPersonaggioItemsByIdPersonaggio(p.getId());

        // 6) Fetch Modificatori
        List<Integer> itemIds = allItems.stream().map(Item::getId).toList();
        List<Modificatore> allMods = modificatoreRepository.findAllByItemIdIn(itemIds);

        List<ItemLabel> abClasse = itemLabelRepository.findByLabelAndItem_IdIn("ABCLASSE", itemIds);

        // 7) Raggruppa Modificatori e Rank in DTO
        Map<String, List<ModificatoreDTO>> modsDtoByStat = allMods.stream()
                .filter(m -> !TipoModificatore.RANK.equals(m.getTipo()))
                .collect(Collectors.groupingBy(
                        m -> m.getStat().getId(),
                        Collectors.mapping(modificatoreMapper::toDTO, Collectors.toList())
                ));
        Map<String, List<RankDTO>> ranksDtoByStat = allMods.stream()
                .filter(m -> TipoModificatore.RANK.equals(m.getTipo()))
                .collect(Collectors.groupingBy(
                        m -> m.getStat().getId(),
                        Collectors.mapping(x -> modificatoreMapper.toRankDTO(x, abClasse), Collectors.toList())
                ));

        // 8) Fetch StatValues con join fetch di Stat
        List<StatValue> stats = statValueRepository.findAllByPersonaggioIdWithStat(id);

        // 9) Costruisci DTO
        DatiPersonaggioDTO dto = new DatiPersonaggioDTO(p);

        // 9a) Calcolo Caratteristiche (sequenziale)
        List<CaratteristicaDTO> carList = stats.stream()
                .filter(sv -> TipoStat.CAR.equals(sv.getStat().getTipo()))
                .map(sv -> modificatoriService.calcolaCaratteristica(
                        sv,
                        modsDtoByStat.getOrDefault(sv.getStat().getId(), Collections.emptyList())
                ))
                .toList();
        dto.getCaratteristiche().addAll(carList);

        // 9b) Calcolo parallelo di Tiri Salvezza e Abilità
        ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        try {
            List<TiroSalvezzaDTO> tsList = pool.submit(() ->
                    stats.stream()
                            .filter(sv -> TipoStat.TS.equals(sv.getStat().getTipo()))
                            .map(sv -> modificatoriService.calcoloTiroSalvezza(
                                    sv,
                                    modsDtoByStat.getOrDefault(sv.getStat().getId(), Collections.emptyList()),
                                    carList
                            ))
                            .toList()
            ).get();
            dto.getTiriSalvezza().addAll(tsList);

            List<AbilitaDTO> abList = pool.submit(() ->
                    stats.stream()
                            .filter(sv -> TipoStat.AB.equals(sv.getStat().getTipo()))
                            .map(sv -> modificatoriService.calcolaAbilita(
                                    sv,
                                    modsDtoByStat.getOrDefault(sv.getStat().getId(), Collections.emptyList()),
                                    ranksDtoByStat.getOrDefault(sv.getStat().getId(), Collections.emptyList()),
                                    carList
                            ))
                            .toList()
            ).get();
            dto.getAbilita().addAll(abList);

            List<ClasseArmaturaDTO> caList = pool.submit(() ->
                    stats.stream()
                            .filter(sv -> TipoStat.CA.equals(sv.getStat().getTipo()))
                            .map(sv -> modificatoriService.calcolaClasseArmatura(
                                    sv,
                                    modsDtoByStat.getOrDefault(sv.getStat().getId(), Collections.emptyList()),
                                    modsDtoByStat.getOrDefault("CA", Collections.emptyList()),
                                    carList
                            ))
                            .toList()
            ).get();
            dto.getClasseArmatura().addAll(caList);

        } catch (Exception e) {
            throw new RuntimeException("Errore nel calcolo parallelo", e);
        } finally {
            pool.shutdown();
        }

        return dto;
    }

}