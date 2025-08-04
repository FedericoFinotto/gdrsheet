package it.fin8.grdsheet.service;

import it.fin8.grdsheet.def.TipoItem;
import it.fin8.grdsheet.def.TipoModificatore;
import it.fin8.grdsheet.dto.*;
import it.fin8.grdsheet.entity.Item;
import it.fin8.grdsheet.entity.ItemLabel;
import it.fin8.grdsheet.entity.StatValue;
import it.fin8.grdsheet.mapper.ItemMapper;
import it.fin8.grdsheet.mapper.ModificatoreMapper;
import it.fin8.grdsheet.repository.ItemRepository;
import it.fin8.grdsheet.repository.ModificatoreRepository;
import it.fin8.grdsheet.repository.PersonaggioRepository;
import it.fin8.grdsheet.repository.StatValueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
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
    @Autowired
    private CalcoloService calcoloService;


    CaratteristicaDTO calcolaCaratteristica(
            StatValue stat,
            List<ModificatoreDTO> modsDto
    ) {
        int base = Integer.parseInt(stat.getValore());
        ModificatoreDTO baseSovrascritto = prendiMaxDTO(modsDto, TipoModificatore.BASE);
        if (baseSovrascritto != null) {
            if (baseSovrascritto.getValore() > base) {
                base = baseSovrascritto.getValore();
            }
        }
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
        modsDto.forEach(x -> {
            if (x.getValore() == null) {
                x.setValore(Integer.parseInt(calcoloService.calcola(x.getFormula(), carList)));
            }
        });
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
        modsDto.forEach(x -> {
            if (x.getValore() == null) {
                x.setValore(Integer.parseInt(calcoloService.calcola(x.getFormula(), carList)));
            }
        });
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
        modsDto.forEach(x -> {
            if (x.getValore() == null) {
                x.setValore(Integer.parseInt(calcoloService.calcola(x.getFormula(), carList)));
            }
        });

        ModificatoreDTO schivare = prendiMaxDTO(modsCADto, TipoModificatore.CA_SCHIVARE);
        ModificatoreDTO armor = prendiMaxDTO(modsCADto, TipoModificatore.CA_ARMOR);
        ModificatoreDTO naturale = prendiMaxDTO(modsCADto, TipoModificatore.CA_NATURALE);
        ModificatoreDTO deviazione = prendiMaxDTO(modsCADto, TipoModificatore.CA_DEVIAZIONE);
        ModificatoreDTO scudo = prendiMaxDTO(modsCADto, TipoModificatore.CA_SHIELD);
        ModificatoreDTO magici = prendiMaxDTO(modsCADto, TipoModificatore.CA_MAGIC);

        List<ModificatoreDTO> valore = modsDto.stream().filter(x -> x.getSempreAttivo() && x.getTipo() == TipoModificatore.VALORE).toList();
        List<ModificatoreDTO> modificatoriAttivi = new ArrayList<>(valore);

        ModificatoreDTO baseMod = carList.stream().filter(c -> c.getId().equals(stat.getMod().getId()))
                .findFirst().map(x -> new ModificatoreDTO(null, x.getId(), x.getModificatore(), null, null, null, true, null)).orElse(null);


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

    BonusAttaccoDTO calcolaBonusAttacco(StatValue stat, List<ModificatoreDTO> modsDto, List<ModificatoreDTO> modsBABDto, List<CaratteristicaDTO> carList, List<ItemLabel> taglie) {
        int modificatore = 0;
        int taglia = getTaglia(taglie);
        List<Integer> attacchiMultipli;

        modsDto.forEach(x -> {
            if (x.getValore() == null) {
                x.setValore(Integer.parseInt(calcoloService.calcola(x.getFormula(), carList)));
            }
        });

        List<ModificatoreDTO> valore = modsDto.stream().filter(x -> x.getSempreAttivo() && x.getTipo() == TipoModificatore.VALORE).toList();
        List<ModificatoreDTO> valoreBAB = modsBABDto.stream().filter(x -> x.getSempreAttivo() && x.getTipo() == TipoModificatore.VALORE).toList();
        Integer modificatoreBAB = valoreBAB.stream().mapToInt(ModificatoreDTO::getValore).sum();
        List<ModificatoreDTO> modificatoriAttivi = new ArrayList<>(valore);

        ModificatoreDTO baseMod = null;
        if (stat.getMod() != null) {
            baseMod = carList.stream().filter(c -> c.getId().equals(stat.getMod().getId()))
                    .findFirst().map(x -> new ModificatoreDTO(null, x.getId(), x.getModificatore(), null, null, null, true, null)).orElse(null);
        }

        if (stat.getStat().getId().equals("BAB")) {
            modificatore = modificatoriAttivi.stream().mapToInt(ModificatoreDTO::getValore).sum();
        }
        if (stat.getStat().getId().equals("LTT")) {
            modificatoriAttivi.addAll(valoreBAB);
            modificatoriAttivi.add(baseMod);
            modificatoriAttivi.add(new ModificatoreDTO(null, stat.getStat().getId(), 4 * taglia, "4*TAGLIA", null, null, true, null));
            modificatore = modificatoriAttivi.stream().mapToInt(ModificatoreDTO::getValore).sum();
        }
        if (stat.getStat().getId().equals("GTT")) {
            modificatoriAttivi.addAll(valoreBAB);
            modificatoriAttivi.add(baseMod);
            modificatoriAttivi.add(new ModificatoreDTO(null, stat.getStat().getId(), -1 * taglia, "-1*TAGLIA", null, null, true, null));
            modificatore = modificatoriAttivi.stream().mapToInt(ModificatoreDTO::getValore).sum();
        }
        if (stat.getStat().getId().equals("MSC")) {
            modificatoriAttivi.addAll(valoreBAB);
            modificatoriAttivi.add(baseMod);
            modificatoriAttivi.add(new ModificatoreDTO(null, stat.getStat().getId(), -1 * taglia, "-1*TAGLIA", null, null, true, null));
            modificatore = modificatoriAttivi.stream().mapToInt(ModificatoreDTO::getValore).sum();
        }

        return new BonusAttaccoDTO(
                stat.getStat().getId(), stat.getStat().getLabel(),
                modificatore, computeIterativeAttacks(modificatoreBAB, modificatore), modificatoriAttivi
        );
    }

    private ModificatoreDTO prendiMaxDTO(List<ModificatoreDTO> mods, TipoModificatore tipo) {
        return mods.stream()
                .filter(m -> tipo.equals(m.getTipo()))
                .max(Comparator.comparing(ModificatoreDTO::getValore))
                .orElse(null);
    }

    private Integer getTaglia(List<ItemLabel> taglia) {
        try {
            // Prima, trova il valore “razza” (preso per primo, ma non lo useremo subito)
            // utile per fallback se non ci sono altri “enabled”
            Optional<ItemLabel> razzaElem = taglia.stream()
                    .filter(c -> c.getItem().getTipo() == TipoItem.RAZZA)
                    .findFirst();

            // Poi cerca il primo item NON di razza e NON disabilitato
            Optional<ItemLabel> primoNonRazzaEnabled = taglia.stream()
                    // escludi la razza
                    .filter(c -> c.getItem().getTipo() != TipoItem.RAZZA)
                    // controlla che NON esista una label “DISABLED” col valore “1”
                    .filter(c -> c.getItem().getLabels().stream()
                            .noneMatch(lbl ->
                                    "DISABLED".equalsIgnoreCase(lbl.getLabel())
                                            && "1".equals(lbl.getValore())
                            ))
                    .findFirst();

            // Scegli l’elemento: se esiste uno non-razza enabled lo prendi, altrimenti la razza
            ItemLabel tagliaSelect = primoNonRazzaEnabled
                    .orElse(razzaElem
                            .orElseThrow(() -> new IllegalStateException("taglia non contiene nemmeno razza!"))
                    );

            // Se vuoi ad esempio la stringa del nome taglia:
            return Integer.parseInt(tagliaSelect.getValore());
        } catch (Exception e) {
            return 0;
        }

    }

    public static List<Integer> computeIterativeAttacks(int baseBab, int baseMod) {
        List<Integer> attacks = new ArrayList<>();
        int value = baseBab;
        while (value > 0) {
            attacks.add(baseMod);
            value -= 5;
            baseMod -= 5;
        }
        return attacks;
    }

    public ContatoreDTO calcolaContatore(StatValue stat,
                                         List<ModificatoreDTO> modsDto,
                                         List<CaratteristicaDTO> carList,
                                         List<Item> livelli) {
        modsDto.forEach(x -> {
            if (x.getValore() == null) {
                x.setValore(Integer.parseInt(calcoloService.calcola(x.getFormula(), carList)));
            }
        });

        int bonus = modsDto.stream()
                .filter(ModificatoreDTO::getSempreAttivo)
                .mapToInt(ModificatoreDTO::getValore)
                .sum();

        int total = bonus;

        if (stat.getMod() != null) {
            CaratteristicaDTO baseCar = carList.stream()
                    .filter(c -> c.getId().equals(stat.getMod().getId()))
                    .findFirst().orElse(null);
            int modBase = baseCar != null ? baseCar.getModificatore() : 0;
            total += modBase;
        }

        if (stat.getFormula() != null) {
            String formula = stat.getFormula().replaceAll("@LVL", String.valueOf(livelli.size() - 1));
            int formulaEvaluated = Integer.parseInt(calcoloService.calcola(formula, carList));
            total += formulaEvaluated;
        }

        return new ContatoreDTO(
                stat.getStat().getId(),
                stat.getStat().getLabel(),
                Integer.parseInt(stat.getValore()),
                total,
                modsDto
        );
    }
}