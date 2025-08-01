package it.fin8.grdsheet.service;

import it.fin8.grdsheet.def.TipoModificatore;
import it.fin8.grdsheet.dto.*;
import it.fin8.grdsheet.entity.StatValue;
import it.fin8.grdsheet.mapper.ItemMapper;
import it.fin8.grdsheet.mapper.ModificatoreMapper;
import it.fin8.grdsheet.repository.ItemRepository;
import it.fin8.grdsheet.repository.ModificatoreRepository;
import it.fin8.grdsheet.repository.PersonaggioRepository;
import it.fin8.grdsheet.repository.StatValueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Component
public class ModificatoriService {

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
    @Autowired
    private ItemMapper itemMapper;


    CaratteristicaDTO calcolaCaratteristica(
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

    TiroSalvezzaDTO calcoloTiroSalvezza(
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

    AbilitaDTO calcolaAbilita(
            StatValue stat,
            List<ModificatoreDTO> modsDto,
            List<RankDTO> ranksDto,
            List<CaratteristicaDTO> carList
    ) {
        String modStatId = stat.getMod() != null ? stat.getMod().getId() : null;
        CaratteristicaDTO cBase = null;
        Integer modBase = 0;
        if (modStatId != null) {
            cBase = carList.stream()
                    .filter(c -> modStatId.equals(c.getId()))
                    .findFirst().orElse(null);
            if (cBase != null) {
                modBase = cBase.getModificatore();
            }
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
                stat, total, modsDto, valoreRank, bonusRank, ranksDto, cBase
        );
    }

    ClasseArmaturaDTO calcolaClasseArmatura(StatValue stat, List<ModificatoreDTO> modsDto, List<ModificatoreDTO> modsCADto, List<CaratteristicaDTO> carList) {
        int modificatore = 0;

        ModificatoreDTO schivare = prendiMaxDTO(modsCADto, TipoModificatore.CA_SCHIVARE);
        ModificatoreDTO armor = prendiMaxDTO(modsCADto, TipoModificatore.CA_ARMOR);
        ModificatoreDTO naturale = prendiMaxDTO(modsCADto, TipoModificatore.CA_NATURALE);
        ModificatoreDTO deviazione = prendiMaxDTO(modsCADto, TipoModificatore.CA_DEVIAZIONE);
        ModificatoreDTO scudo = prendiMaxDTO(modsCADto, TipoModificatore.CA_SHIELD);
        ModificatoreDTO magici = prendiMaxDTO(modsCADto, TipoModificatore.CA_MAGIC);

        List<ModificatoreDTO> valore = modsDto.stream().filter(x -> x.getSempreAttivo() && x.getTipo() == TipoModificatore.VALORE).toList();
        List<ModificatoreDTO> modificatoriAttivi = new ArrayList<>(valore);

        ModificatoreDTO baseMod = carList.stream().filter(c -> c.getId().equals(stat.getMod().getId()))
                .findFirst().map(x -> new ModificatoreDTO(null, x.getId(), x.getModificatore(), null, null, true, null)).orElse(null);


        if (stat.getStat().getId().equals("CA")) {
            modificatoriAttivi.addAll(Stream.of(
                    schivare, armor, naturale, deviazione, scudo, magici, baseMod
            ).filter(Objects::nonNull).toList());
            modificatore = modificatoriAttivi.stream().mapToInt(ModificatoreDTO::getValore).sum();
        }
        if (stat.getStat().getId().equals("CAC")) {
            modificatoriAttivi.addAll(Stream.of(
                    schivare, deviazione, magici, baseMod
            ).filter(Objects::nonNull).toList());
            modificatore = modificatoriAttivi.stream().mapToInt(ModificatoreDTO::getValore).sum();
        }
        if (stat.getStat().getId().equals("CAS")) {
            modificatoriAttivi.addAll(Stream.of(
                    armor, naturale, deviazione, scudo, magici
            ).filter(Objects::nonNull).toList());
            modificatore = modificatoriAttivi.stream().mapToInt(ModificatoreDTO::getValore).sum();
        }

        return new ClasseArmaturaDTO(
                stat.getStat().getId(), stat.getStat().getLabel(),
                modificatore, modificatoriAttivi
        );
    }

    private ModificatoreDTO prendiMaxDTO(List<ModificatoreDTO> mods, TipoModificatore tipo) {
        return mods.stream()
                .filter(m -> tipo.equals(m.getTipo()))
                .max(Comparator.comparing(ModificatoreDTO::getValore))
                .orElse(null);
    }
}