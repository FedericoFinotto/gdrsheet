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
            result.add(cur);
            if (cur.getChild() != null) {
                cur.getChild().stream()
                        .map(Collegamento::getItemTarget)
                        .forEach(stack::push);
            }
        }
        return result;
    }

    /**
     * Ottiene i dati del personaggio con due query principali evitando il MultipleBagFetchException.
     */
    public DatiPersonaggioDTO getDatiPersonaggio(Integer id) {
        // 1) Carica solo Personaggio base
        Personaggio p = personaggioRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Personaggio non trovato"));

        // 2) Carica Items e loro figli in un'unica query (join fetch su child only)
        List<Item> rootItems = itemRepository.findAllByPersonaggioIdWithChild(id);
        List<Item> allItems = flattenItems(rootItems);

        // 3) Carica tutti i modificatori con IN sulla lista di item IDs
        List<Integer> itemIds = allItems.stream()
                .map(Item::getId)
                .toList();
        List<Modificatore> allMods = modificatoreRepository.findAllByItemIdIn(itemIds);
        Map<String, List<Modificatore>> modsByStat = allMods.stream()
                .collect(Collectors.groupingBy(m -> m.getStat().getId()));

        // 4) Carica StatValue con join fetch di Stat e Mod (seconda query)
        List<StatValue> stats = statValueRepository.findAllByPersonaggioIdWithStat(id);

        // 5) Costruzione DTO
        DatiPersonaggioDTO dto = new DatiPersonaggioDTO(p);

        // 5a) Caratteristiche
        stats.stream()
                .filter(sv -> TipoStat.CAR.equals(sv.getStat().getTipo()))
                .map(sv -> calcolaCaratteristica(
                        sv,
                        modsByStat.getOrDefault(sv.getStat().getId(), Collections.emptyList())
                ))
                .forEach(dto.getCaratteristiche()::add);

        // 5b) Tiri Salvezza e Abilità
        stats.forEach(sv -> {
            if (TipoStat.TS.equals(sv.getStat().getTipo())) {
                dto.getTiriSalvezza().add(calcoloTiroSalvezza(
                        sv,
                        modsByStat.getOrDefault(sv.getStat().getId(), Collections.emptyList()),
                        dto.getCaratteristiche()));
            } else if (TipoStat.AB.equals(sv.getStat().getTipo())) {
                dto.getAbilita().add(calcolaAbilita(
                        sv,
                        modsByStat.getOrDefault(sv.getStat().getId(), Collections.emptyList()),
                        dto.getCaratteristiche()));
            }
        });

        return dto;
    }

    // I metodi di calcolo rimangono invariati

    private CaratteristicaDTO calcolaCaratteristica(StatValue stat, List<Modificatore> mods) {
        List<ModificatoreDTO> modsDto = mods.stream()
                .map(modificatoreMapper::toDTO)
                .toList();

        int base = Integer.parseInt(stat.getValore());
        int bonusValore = mods.stream()
                .filter(Modificatore::getSempreAttivo)
                .filter(m -> TipoModificatore.VALORE.equals(m.getTipo()))
                .mapToInt(m -> Integer.parseInt(m.getValore()))
                .sum();
        int valore = base + bonusValore;
        int modificatoreBase = (valore - 10) / 2;
        int bonusMod = mods.stream()
                .filter(Modificatore::getSempreAttivo)
                .filter(m -> TipoModificatore.MOD.equals(m.getTipo()))
                .mapToInt(m -> Integer.parseInt(m.getValore()))
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
            List<Modificatore> mods,
            List<CaratteristicaDTO> caratteristiche
    ) {
        List<ModificatoreDTO> modsDto = mods.stream()
                .map(modificatoreMapper::toDTO)
                .toList();
        int bonus = modsDto.stream()
                .filter(ModificatoreDTO::getSempreAttivo)
                .mapToInt(ModificatoreDTO::getValore)
                .sum();

        CaratteristicaDTO baseCar = caratteristiche.stream()
                .filter(c -> c.getId().equals(stat.getMod().getId()))
                .findFirst()
                .orElse(null);
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
            List<Modificatore> mods,
            List<CaratteristicaDTO> caratteristiche
    ) {
        List<ModificatoreDTO> modsDto = mods.stream()
                .filter(m -> !TipoModificatore.RANK.equals(m.getTipo()))
                .map(modificatoreMapper::toDTO)
                .toList();
        List<RankDTO> ranks = mods.stream()
                .filter(m -> TipoModificatore.RANK.equals(m.getTipo()))
                .map(modificatoreMapper::toRankDTO)
                .toList();

        CaratteristicaDTO baseCar = stat.getMod() != null
                ? caratteristiche.stream()
                .filter(c -> c.getId().equals(stat.getMod().getId()))
                .findFirst().orElse(null)
                : null;
        int modBase = baseCar != null ? baseCar.getModificatore() : 0;

        int bonusVal = modsDto.stream()
                .filter(ModificatoreDTO::getSempreAttivo)
                .mapToInt(ModificatoreDTO::getValore)
                .sum();
        int bonusRank = ranks.stream()
                .filter(RankDTO::getSempreAttivo)
                .mapToInt(RankDTO::getValoreByClasse)
                .sum();
        int valoreRank = ranks.stream()
                .filter(RankDTO::getSempreAttivo)
                .mapToInt(RankDTO::getValore)
                .sum();

        int total = modBase + bonusVal + bonusRank;

        return new AbilitaDTO(
                stat.getStat().getId(),
                stat.getStat().getLabel(),
                baseCar != null ? baseCar.getId() : null,
                modBase,
                total,
                valoreRank,
                bonusRank,
                modsDto,
                ranks
        );
    }
}