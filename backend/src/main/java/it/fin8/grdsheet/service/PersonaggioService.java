package it.fin8.grdsheet.service;

import it.fin8.grdsheet.def.TipoModificatore;
import it.fin8.grdsheet.def.TipoStat;
import it.fin8.grdsheet.dto.*;
import it.fin8.grdsheet.entity.*;
import it.fin8.grdsheet.mapper.ModificatoreMapper;
import it.fin8.grdsheet.repository.ItemRepository;
import it.fin8.grdsheet.repository.ModificatoreRepository;
import it.fin8.grdsheet.repository.PersonaggioRepository;
import it.fin8.grdsheet.repository.StatValueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

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
    private ModificatoreMapper modificatoreMapper;

    /**
     * Flattens iteratively the item hierarchy into a list.
     */
    private List<Item> flattenItems(Collection<Item> rootItems) {
        List<Item> result = new ArrayList<>();
        Deque<Item> stack = new ArrayDeque<>(rootItems);
        while (!stack.isEmpty()) {
            Item cur = stack.pop();
            if (result.add(cur) && cur.getChild() != null) {
                cur.getChild().stream()
                        .map(Collegamento::getItemTarget)
                        .forEach(stack::push);
            }
        }
        return result;
    }

    /**
     * Ottiene i dati del personaggio con calcolo sicuro multithread,
     * evitando accessi Hibernate non thread-safe durante il parallelismo.
     */
    public DatiPersonaggioDTO getDatiPersonaggio(Integer id) {
        // 1) Carica Personaggio
        Personaggio p = personaggioRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Personaggio non trovato"));

        // 2) Fetch Items + figli
        List<Item> rootItems = itemRepository.findAllByPersonaggioIdWithChild(id);
        List<Item> allItems = flattenItems(rootItems);

        // 3) Fetch Modificatori
        List<Integer> itemIds = allItems.stream().map(Item::getId).toList();
        List<Modificatore> allMods = modificatoreRepository.findAllByItemIdIn(itemIds);

        // 4) Raggruppa Modificatori e Rank direttamente da entity
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
                        Collectors.mapping(modificatoreMapper::toRankDTO, Collectors.toList())
                ));

        // 5) Fetch StatValue + Stat + Mod (single query)
        List<StatValue> stats = statValueRepository.findAllByPersonaggioIdWithStat(id);

        // 6) Costruisci DTO personaggio
        DatiPersonaggioDTO dto = new DatiPersonaggioDTO(p);

        // 6a) Calcolo Caratteristiche sequenziale
        List<CaratteristicaDTO> carList = stats.stream()
                .filter(sv -> TipoStat.CAR.equals(sv.getStat().getTipo()))
                .map(sv -> calcolaCaratteristica(
                        sv,
                        modsDtoByStat.getOrDefault(sv.getStat().getId(), Collections.emptyList())
                ))
                .toList();
        dto.getCaratteristiche().addAll(carList);

        // 6b) Calcolo parallelo Tiri Salvezza e Abilità su DTO isolati
        ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        try {
            List<TiroSalvezzaDTO> tsList = pool.submit(() ->
                    stats.parallelStream()
                            .filter(sv -> TipoStat.TS.equals(sv.getStat().getTipo()))
                            .map(sv -> calcoloTiroSalvezza(
                                    sv,
                                    modsDtoByStat.getOrDefault(sv.getStat().getId(), Collections.emptyList()),
                                    carList
                            ))
                            .toList()
            ).get();
            dto.getTiriSalvezza().addAll(tsList);

            List<AbilitaDTO> abList = pool.submit(() ->
                    stats.parallelStream()
                            .filter(sv -> TipoStat.AB.equals(sv.getStat().getTipo()))
                            .map(sv -> calcolaAbilita(
                                    sv,
                                    modsDtoByStat.getOrDefault(sv.getStat().getId(), Collections.emptyList()),
                                    ranksDtoByStat.getOrDefault(sv.getStat().getId(), Collections.emptyList()),
                                    carList
                            ))
                            .toList()
            ).get();
            dto.getAbilita().addAll(abList);

        } catch (Exception e) {
            throw new RuntimeException("Errore nel calcolo parallelo", e);
        } finally {
            pool.shutdown();
        }

        return dto;
    }

    private CaratteristicaDTO calcolaCaratteristica(
            StatValue stat,
            List<ModificatoreDTO> modsDto
    ) {
        int base = Integer.parseInt(stat.getValore());
        int bonusValore = modsDto.stream()
                .filter(ModificatoreDTO::getSempreAttivo)
                .filter(m -> TipoModificatore.VALORE.equals(m.getTipo()))
                .mapToInt(ModificatoreDTO::getValore)
                .sum();
        int valore = base + bonusValore;
        int modificatoreBase = (valore - 10) / 2;
        int bonusMod = modsDto.stream()
                .filter(ModificatoreDTO::getSempreAttivo)
                .filter(m -> TipoModificatore.MOD.equals(m.getTipo()))
                .mapToInt(ModificatoreDTO::getValore)
                .sum();
        int modificatore = modificatoreBase + bonusMod;

        return new CaratteristicaDTO(
                stat.getStat().getId(),
                stat.getStat().getLabel(),
                base,
                valore,
                modificatore,
                modsDto
        );
    }

    private TiroSalvezzaDTO calcoloTiroSalvezza(
            StatValue stat,
            List<ModificatoreDTO> modsDto,
            List<CaratteristicaDTO> carList
    ) {
        int bonus = modsDto.stream()
                .filter(ModificatoreDTO::getSempreAttivo)
                .mapToInt(ModificatoreDTO::getValore)
                .sum();
        CaratteristicaDTO baseCar = carList.stream()
                .filter(c -> c.getId().equals(stat.getMod().getId()))
                .findFirst().orElse(null);
        int modBase = baseCar != null ? baseCar.getModificatore() : 0;
        int total = modBase + bonus;

        return new TiroSalvezzaDTO(
                stat.getStat().getId(),
                stat.getStat().getLabel(),
                baseCar != null ? baseCar.getId() : null,
                modBase,
                total,
                modsDto
        );
    }

    private AbilitaDTO calcolaAbilita(
            StatValue stat,
            List<ModificatoreDTO> modsDto,
            List<RankDTO> ranksDto,
            List<CaratteristicaDTO> carList
    ) {
        // Gestione possible null per stat.getMod()
        String modStatId = stat.getMod() != null ? stat.getMod().getId() : null;
        // Base modificatore dalla caratteristica associata, se presente
        int modBase = 0;
        if (modStatId != null) {
            modBase = carList.stream()
                    .filter(c -> modStatId.equals(c.getId()))
                    .findFirst()
                    .map(CaratteristicaDTO::getModificatore)
                    .orElse(0);
        }
        int bonusVal = modsDto.stream()
                .filter(ModificatoreDTO::getSempreAttivo)
                .mapToInt(ModificatoreDTO::getValore)
                .sum();
        int bonusRank = ranksDto.stream()
                .filter(RankDTO::getSempreAttivo)
                .mapToInt(RankDTO::getModificatore)
                .sum();
        int valoreRank = ranksDto.stream()
                .filter(RankDTO::getSempreAttivo)
                .mapToInt(RankDTO::getValore)
                .sum();
        int total = modBase + bonusVal + bonusRank;

        return new AbilitaDTO(
                stat.getStat().getId(),
                stat.getStat().getLabel(),
                modStatId,
                modBase,
                total,
                valoreRank,
                bonusRank,
                modsDto,
                ranksDto
        );
    }
}
