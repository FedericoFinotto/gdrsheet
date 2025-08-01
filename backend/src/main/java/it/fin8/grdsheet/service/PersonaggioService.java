package it.fin8.grdsheet.service;

import it.fin8.grdsheet.def.TipoItem;
import it.fin8.grdsheet.def.TipoModificatore;
import it.fin8.grdsheet.def.TipoStat;
import it.fin8.grdsheet.dto.*;
import it.fin8.grdsheet.entity.*;
import it.fin8.grdsheet.mapper.ModificatoreMapper;
import it.fin8.grdsheet.repository.ItemRepository;
import it.fin8.grdsheet.repository.PersonaggioRepository;
import it.fin8.grdsheet.repository.StatValueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class PersonaggioService {
    @Autowired
    ItemRepository itemRepository;
    @Autowired
    PersonaggioRepository personaggioRepository;
    @Autowired
    private StatValueRepository statValueRepository;
    @Autowired
    private ModificatoreMapper modificatoreMapper;

    public List<Item> getAllItemsFromPersonaggioId(Integer id) {
        Optional<Personaggio> p = personaggioRepository.findById(id);
        Personaggio personaggio = p.orElse(null);

        if (personaggio != null) {
            return getAllItemsFromPersonaggio(personaggio);
        }
        return List.of();
    }

    // Funzione che prende ricorsivamente tutti gli item di un personaggio e restituisce una lista di ItemDTO
    public List<Item> getAllItemsFromPersonaggio(Personaggio personaggio) {

        List<Item> items = new ArrayList<>();
        Map<Item, Long> classi = new HashMap<>();
        if (personaggio != null && personaggio.getItems() != null) {
            for (Item item : personaggio.getItems()) {
                items.addAll(getAllItemsFromItem(item));
            }

            classi = getClassiFromLivelli(personaggio.getItems().stream().filter(x -> x.getTipo() == TipoItem.LIVELLO).toList());
            for (Map.Entry<Item, Long> entry : classi.entrySet()) {
                Long livello = entry.getValue();
                Item classe = entry.getKey();

                for (Avanzamento av : classe.getAvanzamento()) {
                    if (av.getLivello() <= livello) {
                        items.addAll(getAllItemsFromItem(av.getItemTarget()));
                    }
                }
            }
        }
        return items;
    }

    private Map<Item, Long> getClassiFromLivelli(List<Item> items) {
        return items.stream()
                .filter(x -> x.getTipo() == TipoItem.LIVELLO)  // Filtra solo gli item di tipo LIVELLO
                .map(item -> {
                    // Cerca la classe associata, se presente
                    Optional<Collegamento> classeOpt = item.getChild().stream()
                            .filter(x -> x.getItemTarget().getTipo() == TipoItem.CLASSE)
                            .findFirst();
                    // Se trovi una classe, ritorna l'oggetto Classe, altrimenti null
                    return classeOpt.map(col -> col.getItemTarget()).orElse(null);
                })
                .filter(Objects::nonNull) // Filtra gli item che non hanno una classe
                .collect(Collectors.groupingBy(classe -> classe, Collectors.counting()));
    }

    // Funzione che prende ricorsivamente tutti gli item da un item specifico e include gli ID dei parent
    private List<Item> getAllItemsFromItem(Item item) {
        List<Item> result = new ArrayList<>();
        if (item != null) {
            result.add(item);

            // Se l'item ha dei figli, continua la ricorsione
            if (item.getChild() != null) {
                for (Collegamento child : item.getChild()) {
                    result.addAll(getAllItemsFromItem(child.getItemTarget()));
                }
            }
        }
        return result;
    }

    public DatiPersonaggioDTO getDatiPersonaggio(Integer idPersonaggio) {
        Optional<Personaggio> p = personaggioRepository.findById(idPersonaggio);
        Personaggio personaggio = p.orElse(null);

        if (personaggio == null) {
            throw new RuntimeException("Personaggio non trovato");
        }

        List<Modificatore> modificatori = getAllItemsFromPersonaggioId(idPersonaggio).stream().flatMap(x -> x.getModificatori().stream()).toList();
        List<StatValue> stats = statValueRepository.findAllByPersonaggioId(idPersonaggio);
        DatiPersonaggioDTO dto = new DatiPersonaggioDTO(personaggio);
        for (StatValue stat : stats.stream().filter(x -> TipoStat.CAR.equals(x.getStat().getTipo())).toList()) {
            if (stat.getStat().getTipo().equals(TipoStat.CAR))
                dto.getCaratteristiche().add(calcolaCaratteristica(stat, modificatori));
        }

        for (StatValue stat : stats.parallelStream().filter(x -> !TipoStat.CAR.equals(x.getStat().getTipo())).toList()) {
            if (stat.getStat().getTipo().equals(TipoStat.TS))
                dto.getTiriSalvezza().add(calcoloTiroSalvezza(stat, modificatori, dto.getCaratteristiche()));

            if (stat.getStat().getTipo().equals(TipoStat.AB))
                dto.getAbilita().add(calcolaAbilita(stat, modificatori, dto.getCaratteristiche()));
        }

        return dto;
    }

    private CaratteristicaDTO calcolaCaratteristica(StatValue stat, List<Modificatore> modificatori) {
        List<Modificatore> mods = modificatori.stream().filter(x -> x.getStat().getId().equals(stat.getStat().getId())).toList();
        List<ModificatoreDTO> modificatoriDTO = mods.stream().map(x -> modificatoreMapper.toDTO(x)).toList();
        int bonusValore = mods.stream().filter(x -> x.getSempreAttivo() && x.getTipo().equals(TipoModificatore.VALORE))
                .map(m -> Integer.parseInt(m.getValore()))
                .reduce(0, Integer::sum);
        int bonusModificatore = mods.stream().filter(x -> x.getSempreAttivo() && x.getTipo().equals(TipoModificatore.MOD))
                .map(m -> Integer.parseInt(m.getValore()))
                .reduce(0, Integer::sum);

        int base = Integer.parseInt(stat.getValore());
        int valore = base + bonusValore;
        int modificatoreBase = (int) Math.floor((double) (valore - 10) / 2);
        int modificatore = modificatoreBase + bonusModificatore;

        return new CaratteristicaDTO(stat.getStat().getId(), stat.getStat().getLabel(), base, valore, modificatore, modificatoriDTO);
    }

    private TiroSalvezzaDTO calcoloTiroSalvezza(StatValue stat, List<Modificatore> modificatori, List<CaratteristicaDTO> caratteristiche) {
        List<Modificatore> mods = modificatori.stream().filter(x -> x.getStat().getId().equals(stat.getStat().getId())).toList();
        List<ModificatoreDTO> modificatoriDTO = mods.stream().map(x -> modificatoreMapper.toDTO(x)).toList();
        int bonus = modificatoriDTO.stream()
                .filter(ModificatoreDTO::getSempreAttivo)
                .map(ModificatoreDTO::getValore)
                .reduce(0, Integer::sum);

        Optional<CaratteristicaDTO> caratteristicaBase = caratteristiche.stream().filter(x -> x.getId().equals(stat.getMod().getId())).findFirst();
        String idBase = caratteristicaBase.map(CaratteristicaDTO::getId).orElse(null);
        Integer modBase = caratteristicaBase.map(CaratteristicaDTO::getModificatore).orElse(0);
        int modificatore = caratteristicaBase.map(caratteristicaDTO -> caratteristicaDTO.getModificatore() + bonus).orElse(bonus);

        return new TiroSalvezzaDTO(stat.getStat().getId(), stat.getStat().getLabel(), idBase, modBase, modificatore, modificatoriDTO);
    }

    private AbilitaDTO calcolaAbilita(StatValue stat, List<Modificatore> modificatori, List<CaratteristicaDTO> caratteristiche) {
        List<Modificatore> mods = modificatori.stream().filter(x -> x.getStat().getId().equals(stat.getStat().getId())).toList();

        List<ModificatoreDTO> modificatoreDTOS = mods.stream().filter(x -> !TipoModificatore.RANK.equals(x.getTipo())).map(x -> modificatoreMapper.toDTO(x)).toList();
        List<RankDTO> rankDTOS = mods.stream().filter(x -> TipoModificatore.RANK.equals(x.getTipo())).map(x -> modificatoreMapper.toRankDTO(x)).toList();

        CaratteristicaDTO caratteristicaBase = stat.getMod() != null ? caratteristiche.stream().filter(x -> x.getId().equals(stat.getMod().getId())).findFirst().orElse(null) : null;
        String idBase = caratteristicaBase != null ? caratteristicaBase.getId() : null;
        int modBase = caratteristicaBase != null ? caratteristicaBase.getModificatore() : 0;

        int bonusVALORE = modificatoreDTOS.stream()
                .filter(ModificatoreDTO::getSempreAttivo)
                .map(ModificatoreDTO::getValore)
                .reduce(0, Integer::sum);

        int bonusRANK = rankDTOS.stream()
                .filter(RankDTO::getSempreAttivo)
                .map(RankDTO::getValoreByClasse)
                .reduce(0, Integer::sum);

        int valoreRank = rankDTOS.stream()
                .filter(RankDTO::getSempreAttivo)
                .map(RankDTO::getValore)
                .reduce(0, Integer::sum);

        int modificatore = modBase + bonusVALORE + bonusRANK;

        return new AbilitaDTO(stat.getStat().getId(), stat.getStat().getLabel(), idBase, modBase, modificatore, valoreRank, bonusRANK, modificatoreDTOS, rankDTOS);
    }
}
