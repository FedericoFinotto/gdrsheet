package it.fin8.gdrsheet.service;

import it.fin8.gdrsheet.config.Constants;
import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.def.TipoModificatore;
import it.fin8.gdrsheet.def.TipoStat;
import it.fin8.gdrsheet.dto.*;
import it.fin8.gdrsheet.entity.*;
import it.fin8.gdrsheet.mapper.ItemMapper;
import it.fin8.gdrsheet.mapper.ModificatoreMapper;
import it.fin8.gdrsheet.repository.*;
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

    @Autowired
    private CalcoloService calcoloService;

    @Autowired
    private UtilService utilService;

    /**
     * Flattens iteratively the item hierarchy into a list.
     */
    private List<Item> flattenItems(Collection<Item> rootItems) {
        List<Item> result = new ArrayList<>();
        Deque<Item> stack = new ArrayDeque<>(rootItems);

        while (!stack.isEmpty()) {
            Item cur = stack.pop();

            result.add(cur);
            boolean isDisabled = cur.getLabels() != null &&
                    cur.getLabels().stream().anyMatch(
                            l -> Constants.ITEM_LABEL_DISABILITATO.equalsIgnoreCase(l.getLabel()) && Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE.equals(l.getValore())
                    );

            if (!isDisabled) {
                List<Item> competenze = findCompetenze(cur);
                List<Item> lingue = findLingue(cur);

                if (cur.getChild() != null) {
                    for (Collegamento col : cur.getChild().stream().filter(x -> !x.getItemTarget().getTipo().equals(TipoItem.CLASSE)).toList()) {
                        stack.push(col.getItemTarget());
                    }
                }

                if (competenze != null) {
                    stack.addAll(competenze);
                }

                if (lingue != null) {
                    stack.addAll(lingue);
                }
            }

        }

        return result;
    }


    public ItemsDTO getAllPersonaggioItemsDTOByIdPersonaggio(Integer id) {
        ItemsDTO itemsDTO = new ItemsDTO();
        AllPersonaggioItems allPersonaggioItems = getAllPersonaggioItemsByIdPersonaggio(id);
        for (Item itm : allPersonaggioItems.getItems()) {
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
            if (TipoItem.TRASFORMAZIONE.equals(itm.getTipo())) {
                itemsDTO.getTrasformazioni().add(itemMapper.toDTO(itm));
            }
            if (TipoItem.COMP.equals(itm.getTipo())) {
                itemsDTO.getCompetenze().add(itemMapper.toDTO(itm));
            }
            if (TipoItem.LINGUA.equals(itm.getTipo())) {
                itemsDTO.getLingue().add(itemMapper.toDTO(itm));
            }
        }

        for (Map.Entry<Item, Long> classe : allPersonaggioItems.getClassi().entrySet()) {
            itemsDTO.getClassi().add(itemMapper.toClasseDTO(classe));

            SpellBookDTO spellbook = generateSpellBook(classe.getKey(), classe.getValue(), id);
            if (spellbook != null) {
                itemsDTO.getSpellbooks().add(spellbook);
            }
        }

        for (Map.Entry<Item, List<ItemLivelloDTO>> classeIncantesimo : allPersonaggioItems.getIncantesimi().entrySet()) {
//            Item classe = classeIncantesimo.getKey();
//            List<ItemLivelloDTO> incantesimi = classeIncantesimo.getValue();
//            itemsDTO.getIncantesimi().addAll(incantesimi.stream().map(x -> itemMapper.toIncantesimoDTO(classe, x)).toList());
        }


        return itemsDTO;
    }

    public AllPersonaggioItems getAllPersonaggioItemsByIdPersonaggio(Integer idPersonaggio) {
        AllPersonaggioItems result = new AllPersonaggioItems();

        // Fetch Items principali con un solo join-fetch su child
        List<Item> initialRoots = itemRepository.findAllByPersonaggioIdWithChild(idPersonaggio);

        // Determina le classi e livelli dai soli rootItems
        result.setClassi(calcoloService.getLivelli(initialRoots));

        // Raccogli root items aggiuntive dagli avanzamenti di classe
        List<Item> advanceRoots = new ArrayList<>();
        Item preparedSpell = itemRepository.findItemByNomeAndPersonaggio_Id(Constants.ITEM_INCANTESIMI_PREPARATI, idPersonaggio);
        for (Map.Entry<Item, Long> entry : result.getClassi().entrySet()) {
            Item classe = entry.getKey();
            long livello = entry.getValue();
            advanceRoots.add(classe);

            List<Avanzamento> aumentoStatistiche = new ArrayList<>();
            if (classe.getAvanzamento() != null) {
                for (Avanzamento av : classe.getAvanzamento()) {
                    if (av.getLivello() <= livello) {
                        if (av.getItemTarget().getTipo().equals(TipoItem.AVANZAMENTO)) {
                            aumentoStatistiche.add(av);
                        } else {
                            advanceRoots.add(av.getItemTarget());
                        }
                    }
                }

                aumentoStatistiche.stream()
                        .max(Comparator.comparing(Avanzamento::getLivello)).ifPresent(stat -> advanceRoots.add(stat.getItemTarget()));

            }

        }

        result.setItems(
                flattenItems(
                        Stream.of(initialRoots, advanceRoots)
                                .filter(obj -> true)
                                .flatMap(Collection::stream)
                                .toList()
                )
        );
        return result;
    }

    /**
     * Ottiene i dati del personaggio, includendo items da avanzamenti di classe in base al livello.
     * Unifica i flatten in un solo passaggio per migliorare le prestazioni.
     */
    public DatiPersonaggioDTO getDatiPersonaggio(Integer id) {
        // 1) Carica Personaggio base
        Personaggio p = personaggioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Personaggio non trovato"));

        List<Item> allItems = getAllPersonaggioItemsByIdPersonaggio(p.getId()).getItems();
        List<Item> filteredItems = new ArrayList<>();
        for (Item item : allItems) {
            boolean isDisabled = false;
            for (ItemLabel lbl : item.getLabels()) {
                if (Constants.ITEM_LABEL_DISABILITATO.equalsIgnoreCase(lbl.getLabel()) && Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE.equals(lbl.getValore())) {
                    isDisabled = true;
                    break;
                }
            }
            if (!isDisabled) {
                filteredItems.add(item);
            }
        }
        List<Item> livelloItems = filteredItems.stream().filter(x -> x.getTipo().equals(TipoItem.LIVELLO)).toList();

        // 6) Fetch Modificatori
        List<Integer> itemIds = filteredItems.stream().map(Item::getId).toList();
        filteredItems.forEach(item -> {
            System.out.println(item.getNome());
        });
        List<Modificatore> allMods = modificatoreRepository.findAllByItemIdIn(itemIds);

        List<ItemLabel> abClasse = itemLabelRepository.findByLabelAndItem_IdIn(Constants.ITEM_LABEL_ABILITA_CLASSE, itemIds);
        List<ItemLabel> taglia = itemLabelRepository.findByLabelAndItem_IdIn(Constants.ITEM_LABEL_TAGLIA, itemIds);

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

            List<BonusAttaccoDTO> atkList = pool.submit(() ->
                    stats.stream()
                            .filter(sv -> TipoStat.ATK.equals(sv.getStat().getTipo()))
                            .map(sv -> modificatoriService.calcolaBonusAttacco(
                                    sv,
                                    modsDtoByStat.getOrDefault(sv.getStat().getId(), Collections.emptyList()),
                                    modsDtoByStat.getOrDefault("BAB", Collections.emptyList()),
                                    carList,
                                    taglia
                            ))
                            .toList()
            ).get();
            dto.getBonusAttacco().addAll(atkList);

            List<ContatoreDTO> countList = pool.submit(() ->
                    stats.stream()
                            .filter(sv -> TipoStat.COUNT.equals(sv.getStat().getTipo()))
                            .map(sv -> modificatoriService.calcolaContatore(
                                    sv,
                                    modsDtoByStat.getOrDefault(sv.getStat().getId(), Collections.emptyList()),
                                    carList,
                                    livelloItems
                            ))
                            .toList()
            ).get();
            dto.getContatori().addAll(countList);

            List<AttributoDTO> attrList = pool.submit(() ->
                    stats.stream()
                            .filter(sv -> TipoStat.ATT.equals(sv.getStat().getTipo()))
                            .map(sv -> modificatoriService.calcolaAttributo(
                                    sv,
                                    modsDtoByStat.getOrDefault(sv.getStat().getId(), Collections.emptyList()),
                                    carList
                            ))
                            .toList()
            ).get();
            dto.getAttributi().addAll(attrList);

        } catch (Exception e) {
            throw new RuntimeException("Errore nel calcolo parallelo", e);
        } finally {
            pool.shutdown();
        }

        return dto;
    }

    public Boolean updateHP(UpdateHPRequest upd) {
        Personaggio p = personaggioRepository.findById(upd.getIdPersonaggio()).orElse(null);
        if (p == null) {
            return false;
        }
        StatValue hp = p.getStats().stream().filter(x -> x.getStat().getId().equals("PF")).findFirst().orElse(null);
        StatValue hpTemp = p.getStats().stream().filter(x -> x.getStat().getId().equals("PFTEMP")).findFirst().orElse(null);

        if (hpTemp == null || hp == null) {
            return false;
        }

        hp.setValore(upd.getPf());
        hpTemp.setValore(upd.getPfTemp());

        try {
            statValueRepository.save(hp);
            statValueRepository.save(hpTemp);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public Boolean updateContatore(UpdateCounterRequest upd) {
        Personaggio p = personaggioRepository.findById(upd.getIdPersonaggio()).orElse(null);
        if (p == null) {
            return false;
        }
        StatValue stat = p.getStats().stream().filter(x -> x.getStat().getId().equals(upd.getId())).findFirst().orElse(null);

        if (stat == null) {
            return false;
        }

        stat.setValore(upd.getValore());

        try {
            statValueRepository.save(stat);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private List<Item> findCompetenze(Item item) {
        ItemLabel competenze = item.getLabels().stream().filter(x -> x.getLabel().equals(Constants.ITEM_LABEL_LISTA_COMPETENZE)).findFirst().orElse(null);
        if (competenze != null) {
            List<Integer> competenzeIds = Stream.of(competenze.getValore().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .toList();
            return itemRepository.findItemsByIds(competenzeIds);
        }
        return new ArrayList<>();
    }

    private List<Item> findLingue(Item item) {
        ItemLabel lingue = item.getLabels().stream().filter(x -> x.getLabel().equals(Constants.ITEM_LABEL_LISTA_LINGUE)).findFirst().orElse(null);
        if (lingue != null) {
            List<Integer> lingueIds = Stream.of(lingue.getValore().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::parseInt)
                    .toList();
            return itemRepository.findItemsByIds(lingueIds);
        }
        return new ArrayList<>();
    }

    private List<ItemLivelloDTO> findIncantesimi(Item item, Integer idPersonaggio) {
        ItemLabel spellDiClasse = item.getLabels().stream().filter(x -> x.getLabel().equals(Constants.ITEM_LABEL_LISTA_INCANTESIMI)).findFirst().orElse(null);
        if (spellDiClasse != null) {
            ItemLabel preparedSpell = itemLabelRepository.findByItemPersonaggioIdAndItemNome(idPersonaggio, Constants.ITEM_INCANTESIMI_PREPARATI).stream().filter(x -> x.getLabel().equals(spellDiClasse.getValore())).findFirst().orElse(null);
            if (preparedSpell != null) {
                List<Integer> incantesimiIds = Stream.of(preparedSpell.getValore().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Integer::parseInt)
                        .toList();
                List<ItemLivelloDTO> incantesimi = itemRepository.findIncantesimiWithLivelloByLabelAndIds(spellDiClasse.getValore(), incantesimiIds);

                if (!incantesimi.isEmpty()) {
                    incantesimi.forEach(itm -> {
                        itm.getItem().getLabels().add(new ItemLabel(null, null, Constants.ITEM_LABEL_LIVELLO_INCANTESIMO, itm.getLivello()));
                        itm.getItem().getLabels().add(new ItemLabel(null, null, Constants.ITEM_LABEL_CLASSE_INCANTESIMO, spellDiClasse.getValore()));
                    });
                    return incantesimi;
                }
            }
        }
        return new ArrayList<>();
    }

    private SpellBookDTO generateSpellBook(Item classe, Long lvl, Integer idPersonaggio) {
        ItemLabel spellList = classe.getLabels().stream().filter(x -> x.getLabel().equals(Constants.ITEM_LABEL_LISTA_INCANTESIMI)).findFirst().orElse(null);
        if (spellList == null) return null;
        SpellBookDTO spellBook = new SpellBookDTO();
        spellBook.setIdClasse(classe.getId());
        spellBook.setNomeClasse(classe.getNome());
        spellBook.setSpellList(spellList.getValore());
        String slotBonus = utilService.getItemLabel(classe, Constants.ITEM_LABEL_SPELL_SLOT_BONUS);

        Avanzamento avanzamento = classe.getAvanzamento().stream().filter(y -> y.getItemTarget().getTipo().equals(TipoItem.AVANZAMENTO) && Math.toIntExact(lvl) == y.getLivello()).findFirst().orElse(null);
        Item preparedSpell = itemRepository.findItemByNomeAndPersonaggio_Id(Constants.ITEM_INCANTESIMI_PREPARATI, idPersonaggio);
        List<SpellBookIncantesimoDTO> incantesimi = preparedSpell.getChild().stream().map(x -> itemMapper.toIncantesimoDTO(classe, x)).toList();
        if (avanzamento != null) {
            ItemLabel spellSlot = avanzamento.getItemTarget().getLabels().stream().filter(x -> x.getLabel().equals(Constants.ITEM_LABEL_SPELL_SLOT)).findFirst().orElse(null);
            if (spellSlot != null) {
                List<Integer> slots = Arrays.stream(spellSlot.getValore().split(","))
                        .map(String::trim) // rimuove spazi eventuali
                        .map(Integer::parseInt) // converte in Integer
                        .toList();


                for (int i = 0; i < slots.size(); i++) {
                    if (slots.get(i) > 0) {
                        SpellBookLivelloDTO spellBookLivello = new SpellBookLivelloDTO();
                        spellBookLivello.setLivello(i);
                        spellBookLivello.setSlot(slots.get(i));
                        if (slotBonus != null && i > 0) {
                            spellBookLivello.getBonus().add(slotBonus);
                        }
                        int finalI = i;
                        spellBookLivello.getIncantesimi().addAll(incantesimi.stream().filter(x -> x.getLivello() == finalI && x.getSpellList().equals(spellList.getValore())).toList());
                        spellBook.getLivelli().add(spellBookLivello);
                    }
                }

            }
        }
        return spellBook;
    }

    public void updateBaseStatValue(UpdateBaseStatValueRequest request) {
        Personaggio p = personaggioRepository.findById(request.getIdPersonaggio()).orElse(null);
        if (p == null) throw new RuntimeException("Personaggio non trovato");

        StatValue s = p.getStats().stream().filter(x -> x.getStat().getId().equals(request.getIdStat())).findFirst().orElse(null);
        if (s == null) throw new RuntimeException("Stat non trovato");

        s.setValore(request.getValore().toString());
        statValueRepository.save(s);
    }

}