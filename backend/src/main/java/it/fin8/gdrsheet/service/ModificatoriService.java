package it.fin8.gdrsheet.service;

import it.fin8.gdrsheet.config.Constants;
import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.def.TipoModificatore;
import it.fin8.gdrsheet.dto.*;
import it.fin8.gdrsheet.entity.*;
import it.fin8.gdrsheet.repository.ItemRepository;
import it.fin8.gdrsheet.repository.PersonaggioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ModificatoriService {

    @Autowired
    private PersonaggioRepository personaggioRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CalcoloService calcoloService;

    @Autowired
    private UtilService utilService;


    CaratteristicaDTO calcolaCaratteristica(
            StatValue stat,
            List<ModificatoreDTO> modsDto
    ) {
        List<ModificatoreDTO> modificatoriAttivi = new ArrayList<>();
        if (!stat.getValore().equals("0")) {
            modificatoriAttivi.add(new ModificatoreDTO(null, stat.getStat().getId(), Integer.parseInt(stat.getValore()), null, null, TipoModificatore.VALORE, false, "Temporaneo", null));
        }
        modificatoriAttivi.add(prendiMaxDTO(modsDto, TipoModificatore.BASE));
        int valoreBase = (modsDto.stream().filter(x -> x.getTipoItem().equals(TipoItem.LIVELLO)).mapToInt(ModificatoreDTO::getValore).sum());

        modificatoriAttivi.addAll(modsDto.stream().filter(m -> TipoModificatore.VALORE.equals(m.getTipo())).toList());

        int valore = Optional.of(modificatoriAttivi)
                .orElse(Collections.emptyList())
                .stream()
                .filter(x -> x.getNota() == null)
                .mapToInt(ModificatoreDTO::getValore)
                .sum();

        int valorePermanente = Optional.of(modificatoriAttivi)
                .orElse(Collections.emptyList())
                .stream()
                .filter(x -> x.getNota() == null && x.getPermanente())
                .mapToInt(ModificatoreDTO::getValore)
                .sum();

        if (modificatoriAttivi.stream().filter(x -> x.getNota() == null && x.getPermanente()).noneMatch(x -> x.getTipo().equals(TipoModificatore.BASE))) {
            valorePermanente += valoreBase;
        }


        return new CaratteristicaDTO(
                stat.getStat().getId(),
                stat.getStat().getLabel(),
                valore,
                (valore - 10) / 2,
                (valorePermanente - 10) / 2,
                modificatoriAttivi
        );
    }

    TiroSalvezzaDTO calcoloTiroSalvezza(
            StatValue stat,
            List<ModificatoreDTO> modsDto,
            List<CaratteristicaDTO> carList
    ) {
        applicaCalcoli(modsDto, carList);
        List<ModificatoreDTO> modificatoriAttivi = new ArrayList<>();
        addModificatoreFromCaratteristica(modificatoriAttivi, stat, carList);
        modificatoriAttivi.addAll(modsDto);

        if (!stat.getValore().equals("0")) {
            modificatoriAttivi.add(new ModificatoreDTO(null, stat.getStat().getId(), Integer.parseInt(stat.getValore()), null, null, TipoModificatore.VALORE, false, "Temporaneo", null));
        }

        int modificatore = modificatoriAttivi.stream()
                .filter(x -> x.getNota() == null)
                .mapToInt(ModificatoreDTO::getValore)
                .sum();

        return new TiroSalvezzaDTO(
                stat.getStat().getId(),
                stat.getStat().getLabel(),
                modificatore,
                modificatoriAttivi
        );
    }

    AbilitaDTO calcolaAbilita(
            StatValue stat,
            List<ModificatoreDTO> modsDto,
            List<RankDTO> ranksDto,
            List<CaratteristicaDTO> carList
    ) {
        applicaCalcoli(modsDto, carList);
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
                .filter(x -> x.getNota() == null)
                .mapToInt(ModificatoreDTO::getValore)
                .sum();
        int bonusRank = ranksDto.stream()
                .filter(RankDTO::getSempreAttivo)
                .collect(Collectors.teeing(
                        Collectors.summingInt(r -> r.getDiClasse() ? r.getValore() : 0),
                        Collectors.summingInt(r -> r.getDiClasse() ? 0 : r.getValore()),
                        (cls, non) -> cls + non / 2
                ));

        int valoreRank = ranksDto.stream()
                .filter(RankDTO::getSempreAttivo)
                .mapToInt(RankDTO::getValore)
                .sum();
        int total = modBase + bonusVal + bonusRank;

        return new AbilitaDTO(
                stat, total, modsDto, valoreRank, bonusRank, ranksDto, cBase
        );
    }

    ClasseArmaturaDTO calcolaClasseArmatura(StatValue stat, List<ModificatoreDTO> modsDto, List<ModificatoreDTO> modsCADto, List<CaratteristicaDTO> carList, List<ItemLabel> taglie) {
        int modificatore = 0;
        applicaCalcoli(modsDto, carList);
        int taglia = getTaglia(taglie);

        ModificatoreDTO schivare = prendiMaxDTO(modsCADto, TipoModificatore.CA_SCHIVARE);
        ModificatoreDTO armor = prendiMaxDTO(modsCADto, TipoModificatore.CA_ARMOR);
        ModificatoreDTO naturale = prendiMaxDTO(modsCADto, TipoModificatore.CA_NATURALE);
        ModificatoreDTO deviazione = prendiMaxDTO(modsCADto, TipoModificatore.CA_DEVIAZIONE);
        ModificatoreDTO scudo = prendiMaxDTO(modsCADto, TipoModificatore.CA_SHIELD);
        ModificatoreDTO magici = prendiMaxDTO(modsCADto, TipoModificatore.CA_MAGIC);

        List<ModificatoreDTO> valore = modsDto.stream().filter(x -> x.getNota() == null && x.getTipo() == TipoModificatore.VALORE).toList();
        List<ModificatoreDTO> valoreCA = modsCADto.stream().filter(x -> x.getNota() == null && x.getTipo() == TipoModificatore.VALORE).toList();
        List<ModificatoreDTO> modificatoriAttivi = new ArrayList<>(valore);
        modificatoriAttivi.add(new ModificatoreDTO(null, stat.getStat().getId(), -1 * taglia, "-1*TAGLIA", null, null, true, "Taglia", null));

        ModificatoreDTO baseMod = carList.stream().filter(c -> c.getId().equals(stat.getMod().getId()))
                .findFirst().map(x -> new ModificatoreDTO(null, x.getId(), x.getModificatore(), null, null, null, true, x.getLabel(), null)).orElse(null);

        if (!stat.getValore().equals("0")) {
            modificatoriAttivi.add(new ModificatoreDTO(null, stat.getStat().getId(), Integer.parseInt(stat.getValore()), null, null, TipoModificatore.VALORE, false, "Temporaneo", null));
        }


        if (stat.getStat().getId().equals("CA")) {
            modificatoriAttivi.addAll(Stream.of(
                    baseMod, schivare, armor, naturale, deviazione, scudo, magici
            ).filter(Objects::nonNull).toList());
            modificatore = modificatoriAttivi.stream().mapToInt(ModificatoreDTO::getValore).sum();
        }
        if (stat.getStat().getId().equals("CAC")) {
            modificatoriAttivi.addAll(valoreCA);
            modificatoriAttivi.addAll(Stream.of(
                    baseMod, schivare, deviazione, magici
            ).filter(Objects::nonNull).toList());
            modificatore = modificatoriAttivi.stream().mapToInt(ModificatoreDTO::getValore).sum();
        }
        if (stat.getStat().getId().equals("CAS")) {
            modificatoriAttivi.addAll(valoreCA);
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

        applicaCalcoli(modsDto, carList);

        List<ModificatoreDTO> valore = modsDto.stream().filter(x -> x.getNota() == null && x.getTipo() == TipoModificatore.VALORE).toList();
        List<ModificatoreDTO> valoreBAB = modsBABDto.stream().filter(x -> x.getNota() == null && x.getTipo() == TipoModificatore.VALORE).toList();
        int modificatoreBAB = valoreBAB.stream().mapToInt(ModificatoreDTO::getValore).sum();
        List<ModificatoreDTO> modificatoriAttivi = new ArrayList<>(valore);

        ModificatoreDTO baseMod = null;
        if (stat.getMod() != null) {
            baseMod = carList.stream().filter(c -> c.getId().equals(stat.getMod().getId()))
                    .findFirst().map(x -> new ModificatoreDTO(null, x.getId(), x.getModificatore(), null, null, null, true, x.getLabel(), null)).orElse(null);
        }

        if (!stat.getValore().equals("0")) {
            modificatoriAttivi.add(new ModificatoreDTO(null, stat.getStat().getId(), Integer.parseInt(stat.getValore()), null, null, TipoModificatore.VALORE, false, "Temporaneo", null));
        }

        if (stat.getStat().getId().equals("BAB")) {
            modificatore = Optional.of(modificatoriAttivi)
                    .map(list -> list.stream()
                            .mapToInt(ModificatoreDTO::getValore)
                            .sum())
                    .orElse(0);
        }
        if (stat.getStat().getId().equals("LTT")) {
            modificatoriAttivi.addAll(valoreBAB);
            modificatoriAttivi.add(baseMod);
            modificatoriAttivi.add(new ModificatoreDTO(null, stat.getStat().getId(), 4 * taglia, "4*TAGLIA", null, null, true, "Taglia", null));
            modificatore = Optional.of(modificatoriAttivi)
                    .map(list -> list.stream()
                            .mapToInt(ModificatoreDTO::getValore)
                            .sum())
                    .orElse(0);
        }
        if (stat.getStat().getId().equals("GTT")) {
            modificatoriAttivi.addAll(valoreBAB);
            modificatoriAttivi.add(baseMod);
            modificatoriAttivi.add(new ModificatoreDTO(null, stat.getStat().getId(), -1 * taglia, "-1*TAGLIA", null, null, true, "Taglia", null));
            modificatore = Optional.of(modificatoriAttivi)
                    .map(list -> list.stream()
                            .mapToInt(ModificatoreDTO::getValore)
                            .sum())
                    .orElse(0);
        }
        if (stat.getStat().getId().equals("MSC")) {
            modificatoriAttivi.addAll(valoreBAB);
            modificatoriAttivi.add(baseMod);
            modificatoriAttivi.add(new ModificatoreDTO(null, stat.getStat().getId(), -1 * taglia, "-1*TAGLIA", null, null, true, "Taglia", null));
            modificatore = Optional.of(modificatoriAttivi)
                    .map(list -> list.stream()
                            .mapToInt(ModificatoreDTO::getValore)
                            .sum())
                    .orElse(0);

        }

        return new BonusAttaccoDTO(
                stat.getStat().getId(), stat.getStat().getLabel(),
                modificatore, computeIterativeAttacks(modificatoreBAB, modificatore), modificatoriAttivi
        );
    }

    public ContatoreDTO calcolaContatore(StatValue stat,
                                         List<ModificatoreDTO> modsDto,
                                         List<CaratteristicaDTO> carList,
                                         DadiVitaDTO dadiVita) {
        applicaCalcoli(modsDto, carList);

        List<ModificatoreDTO> modificatoriAttivi = new ArrayList<>(modsDto.stream().toList());

        if (stat.getMod() != null) {
            carList.stream()
                    .filter(c -> c.getId().equals(stat.getMod().getId()))
                    .findFirst().ifPresent(baseCar ->
                            modificatoriAttivi.add(new ModificatoreDTO(null, stat.getMod().getId(), baseCar.getModificatore(), null, null, null, true, stat.getMod().getLabel(), null)));
        }

        if (stat.getFormula() != null) {
            String formula = stat.getFormula().replaceAll("@DV", String.valueOf(dadiVita.getTotale()));
            int formulaEvaluated = Integer.parseInt(calcoloService.calcola(formula, carList));
            modificatoriAttivi.add(new ModificatoreDTO(null, null, formulaEvaluated, null, null, null, true, "BASE", null));
        }

        int total = modificatoriAttivi.stream()
                .filter(x -> x.getNota() == null)
                .mapToInt(ModificatoreDTO::getValore)
                .sum();

        return new ContatoreDTO(
                stat.getStat().getId(),
                stat.getStat().getLabel(),
                Integer.parseInt(stat.getValore()),
                total,
                modsDto
        );
    }

    AttributoDTO calcolaAttributo(
            StatValue stat,
            List<ModificatoreDTO> modsDto,
            List<CaratteristicaDTO> carList
    ) {
        int modificatore = 0;
        applicaCalcoli(modsDto, carList);

        List<ModificatoreDTO> modificatoriAttivi = new ArrayList<>();
        if (!stat.getValore().equals("0")) {
            modificatoriAttivi.add(new ModificatoreDTO(null, stat.getStat().getId(), Integer.parseInt(stat.getValore()), null, null, TipoModificatore.VALORE, false, "Temporaneo", null));
        }
        modificatoriAttivi.addAll(new ArrayList<>(modsDto.stream().filter(x -> x.getNota() == null && x.getTipo() == TipoModificatore.VALORE).toList()));

        if (stat.getMod() != null) {
            ModificatoreDTO baseMod = carList.stream().filter(c -> c.getId().equals(stat.getMod().getId()))
                    .findFirst().map(x -> new ModificatoreDTO(null, x.getId(), x.getModificatore(), null, null, null, true, x.getLabel(), null)).orElse(null);
            modificatoriAttivi.add(baseMod);
        }

        modificatore = modificatoriAttivi.stream().filter(x -> x.getNota() == null).mapToInt(ModificatoreDTO::getValore).sum();

        return new AttributoDTO(
                stat.getStat().getId(), stat.getStat().getLabel(),
                modificatore, modificatoriAttivi
        );
    }

    DadiVitaDTO calcolaDadiVita(
            StatValue stat,
            List<ModificatoreDTO> modsDto,
            List<CaratteristicaDTO> carList
    ) {
        applicaCalcoli(modsDto, carList);

        List<ModificatoreDTO> modificatoriAttivi = new ArrayList<>();
        if (!stat.getValore().equals("0")) {
            modificatoriAttivi.add(new ModificatoreDTO(null, stat.getStat().getId(), Integer.parseInt(stat.getValore()), null, null, TipoModificatore.VALORE, false, "Temporaneo", null));
        }
        modificatoriAttivi.addAll(new ArrayList<>(modsDto.stream().filter(x -> x.getNota() == null && x.getTipo() == TipoModificatore.VALORE).toList()));

        DiceSummary dice = utilService.combineDice(modificatoriAttivi, ModificatoreDTO::getFormula);

        if (stat.getMod() != null) {
            ModificatoreDTO baseMod = carList.stream().filter(c -> c.getId().equals(stat.getMod().getId()))
                    .findFirst().map(x -> new ModificatoreDTO(null, x.getId(), x.getModificatore(), null, null, null, true, x.getLabel(), null)).orElse(null);
            modificatoriAttivi.add(baseMod);
        }

        return new DadiVitaDTO(
                stat.getStat().getId(), stat.getStat().getLabel(), dice.getTotalN(), dice.getCombined(), modificatoriAttivi
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
                                    Constants.ITEM_LABEL_DISABILITATO.equalsIgnoreCase(lbl.getLabel())
                                            && Constants.ITEM_LABEL_DISABILITATO_VALORE_TRUE.equals(lbl.getValore())
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

    private void applicaCalcoli(List<ModificatoreDTO> modsDto, List<CaratteristicaDTO> carList) {
        modsDto.forEach(x -> {
            try {
                if (x.getValore() == null) {
                    x.setValore(Integer.parseInt(calcoloService.calcola(x.getFormula(), carList)));
                }
            } catch (Exception e) {
                x.setFormula(x.getFormula());
                x.setValore(0);
            }

        });
    }

    private void addModificatoreFromCaratteristica(List<ModificatoreDTO> modificatoriAttivi, StatValue stat, List<CaratteristicaDTO> carList) {
        if (stat.getMod() != null) {
            carList.stream()
                    .filter(c -> c.getId().equals(stat.getMod().getId()))
                    .findFirst()
                    .ifPresent(c -> modificatoriAttivi.add(
                            new ModificatoreDTO(
                                    null,
                                    stat.getStat().getId(),
                                    c.getModificatore(),
                                    null,
                                    null,
                                    TipoModificatore.VALORE,
                                    true,
                                    stat.getMod().getLabel(), null
                            )
                    ));
        }
    }

    public List<AbilitaClasseDTO> getAbilitaClasse(Integer idPersonaggio, Integer livello, Integer idClasse) {
        Personaggio personaggio = personaggioRepository.findById(idPersonaggio)
                .orElseThrow(() -> new RuntimeException("Personaggio non trovato"));

        // Tutti i livelli attivi fino a "livello"
        List<Item> livelli = personaggio.getItems().stream()
                .filter(x -> x.getTipo().equals(TipoItem.LIVELLO))
                .filter(x -> {
                    String lv = x.getLabel(Constants.ITEM_LIVELLO_LVL);
                    try {
                        return lv != null && Integer.parseInt(lv) <= livello;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                })
                .filter(x -> {
                    String dis = x.getLabel(Constants.ITEM_LABEL_DISABILITATO);
                    return dis == null || Objects.equals(dis, Constants.ITEM_LABEL_DISABILITATO_VALORE_FALSE);
                })
                .toList();

        List<AbilitaClasseDTO> abilitaClasse = new ArrayList<>();
        List<Item> classiUniche;

        if (!livelli.isEmpty()) {
            // 1) prendi le CLASSI una sola volta (deduplicate per id)
            classiUniche =
                    livelli.stream()
                            .filter(Objects::nonNull)
                            .flatMap(lv -> Optional.ofNullable(lv.getChild()).stream().flatMap(List::stream))
                            .map(Collegamento::getItemTarget)
                            .filter(it -> it.getTipo() == TipoItem.CLASSE)
                            .collect(Collectors.collectingAndThen(
                                    Collectors.toCollection(() -> new java.util.TreeSet<>(Comparator.comparing(Item::getId))),
                                    ArrayList::new
                            ));

            if (classiUniche.stream().noneMatch(x -> x.getId().equals(idClasse))) {
                classiUniche.add(itemRepository.findById(idClasse)
                        .orElseThrow(() -> new RuntimeException("Classe non trovata")));
            }

            // 2) indice abilità -> DTO aggregato
            Map<String, AbilitaClasseDTO> index = new LinkedHashMap<>();

            for (Item cl : classiUniche) {
                String abClasse = cl.getLabel(Constants.ITEM_LABEL_ABILITA_CLASSE);
                if (abClasse == null || abClasse.isBlank()) continue;

                Arrays.stream(abClasse.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .forEach(tok -> {
                            boolean all = tok.contains("!");
                            boolean classeRichiesta = cl.getId().equals(idClasse);
                            String idAb = tok.replace("!", "").trim();

                            AbilitaClasseDTO dto = index.computeIfAbsent(
                                    idAb,
                                    k -> new AbilitaClasseDTO(new ArrayList<>(), k, false, false)
                            );

                            // aggiungi classe se non già presente
                            List<IdNomeDTO> cls = dto.getClasse();
                            boolean presente = cls.stream().anyMatch(c -> Objects.equals(c.getId(), cl.getId()));
                            if (!presente) {
                                cls.add(new IdNomeDTO(cl.getId(), cl.getNome()));
                            }

                            if (all) dto.setAll(true);
                            if (classeRichiesta) dto.setDiClasse(true);
                        });
            }

            abilitaClasse.addAll(index.values());
        }

        return abilitaClasse;
    }

    public void applicaSinergie(DatiPersonaggioDTO dp) {
        for (AbilitaDTO ab : dp.getAbilita()) {
            List<Sinergia> sinergie = listaSinergie().stream().filter(x -> x.getT().equals(ab.getAbilita().getId())).toList();
            for (Sinergia s : sinergie) {
                AbilitaDTO abilitaSource = dp.getAbilita().stream().filter(x -> x.getAbilita().getId().equals(s.getS())).findFirst().orElse(null);
                if (abilitaSource != null) {
                    if (abilitaSource.getRank().getModificatore() >= 5) {
                        if (ab.getAbilita().getModificatori() == null || ab.getAbilita().getModificatori().isEmpty()) {
                            ab.getAbilita().setModificatori(new ArrayList<>());
                        }
                        ab.getAbilita().getModificatori().add(s.getM());
                    }
                }
            }
        }
    }

    private List<Sinergia> listaSinergie() {
        List<Sinergia> sinergie = new ArrayList<>();
        // TODO: Gestire Artigianato e Valutare diversi e aggiungere Sinergie
        sinergie.add(new Sinergia("AB26", "AB12", new ModificatoreDTO(null, "AB12", 2, "+2", null, TipoModificatore.VALORE, true, "Sinergia Raggirare", null))); //Diplomazia
        sinergie.add(new Sinergia("AB26", "AB17", new ModificatoreDTO(null, "AB17", 2, "+2", null, TipoModificatore.VALORE, true, "Sinergia Raggirare", null))); //Intimidire
        sinergie.add(new Sinergia("AB26", "AB6", new ModificatoreDTO(null, "AB6", 2, "+2", "Quando reciti un ruolo", TipoModificatore.VALORE, true, "Sinergia Raggirare", null))); //Camuffare
        sinergie.add(new Sinergia("AB26", "AB27", new ModificatoreDTO(null, "AB27", 2, "+2", null, TipoModificatore.VALORE, true, "Sinergia Raggirare", null))); //Rapidità di Mano
        sinergie.add(new Sinergia("AB11", "AB34", new ModificatoreDTO(null, "AB34", 2, "+2", "Quando usi Pergamene", TipoModificatore.VALORE, true, "Sinergia Decifrare Scritture", null))); //Utilizzare Oggetti Magici
        sinergie.add(new Sinergia("AB4", "AB33", new ModificatoreDTO(null, "AB33", 2, "+2", "legature/vincoli", TipoModificatore.VALORE, true, "Sinergia Artista della fuga", null))); //Usare Corde
        sinergie.add(new Sinergia("AB2", "AB7", new ModificatoreDTO(null, "AB7", 2, "+2", null, TipoModificatore.VALORE, true, "Sinergia Addestrare Animali", null))); //Cavalcare
        sinergie.add(new Sinergia("AB2", "AB37", new ModificatoreDTO(null, "AB37", 2, "+2", null, TipoModificatore.VALORE, true, "Sinergia Addestrare Animali", null))); //Empatia Selvatica
        sinergie.add(new Sinergia("AB28", "AB1", new ModificatoreDTO(null, "AB1", 2, "+2", null, TipoModificatore.VALORE, true, "Sinergia Saltare", null))); //Acrobazia
        sinergie.add(new Sinergia("CO06", "AB29", new ModificatoreDTO(null, "AB29", 2, "+2", null, TipoModificatore.VALORE, true, "Sinergia Conoscenze Arcane", null))); //Sapienza Magica
        sinergie.add(new Sinergia("CO09", "AB8", new ModificatoreDTO(null, "AB8", 2, "+2", "porte segrete/scomparti simili", TipoModificatore.VALORE, true, "Sinergia Conoscenze (architettura e ingegneria)", null))); //Cercare
        sinergie.add(new Sinergia("CO01", "AB32", new ModificatoreDTO(null, "AB32", 2, "+2", "Sottoterra", TipoModificatore.VALORE, true, "Sinergia Conoscenze (dungeon)", null))); //Sopravvivenza
        sinergie.add(new Sinergia("CO04", "AB32", new ModificatoreDTO(null, "AB32", 2, "+2", "orientarsi/evitare pericoli", TipoModificatore.VALORE, true, "Sinergia Conoscenze (geografia)", null))); //Sopravvivenza
        sinergie.add(new Sinergia("CO11", "C012", new ModificatoreDTO(null, "C012", 2, "+2", null, TipoModificatore.VALORE, true, "Sinergia Conoscenze (Storia)", null))); //Conoscenze Bardiche
        sinergie.add(new Sinergia("CO07", "AB25", new ModificatoreDTO(null, "AB25", 2, "+2", null, TipoModificatore.VALORE, true, "Sinergia Conoscenze (locale)", null))); //Raccogliere Informazioni
        sinergie.add(new Sinergia("CO10", "AB32", new ModificatoreDTO(null, "AB32", 2, "+2", "ambienti naturali all’aperto", TipoModificatore.VALORE, true, "Sinergia Conoscenze (natura)", null))); //Sopravvivenza
        sinergie.add(new Sinergia("CO02", "AB12", new ModificatoreDTO(null, "AB12", 2, "+2", null, TipoModificatore.VALORE, true, "Sinergia Conoscenze (nobiltà e regalità)", null))); //Diplomazia
        sinergie.add(new Sinergia("CO08", "CO08", new ModificatoreDTO(null, "B", 2, "+2", "Prove per scacciare non morti", TipoModificatore.VALORE, true, "?? Sinergia Conoscenze Religioni", null)));
        sinergie.add(new Sinergia("CO05", "AB32", new ModificatoreDTO(null, "AB32", 2, "+2", "su altri piani", TipoModificatore.VALORE, true, "Sinergia Conoscenze (i piani) ", null))); //Sopravvivenza
        sinergie.add(new Sinergia("AB23", "AB12", new ModificatoreDTO(null, "AB12", 2, "+2", null, TipoModificatore.VALORE, true, "Sinergia Percepire Intenzioni", null))); //Diplomazia
        sinergie.add(new Sinergia("AB1", "AB14", new ModificatoreDTO(null, "AB14", 2, "+2", null, TipoModificatore.VALORE, true, "Sinergia Acrobazia", null))); //Equilibrio
        sinergie.add(new Sinergia("AB1", "AB28", new ModificatoreDTO(null, "AB28", 2, "+2", null, TipoModificatore.VALORE, true, "Sinergia Acrobazia", null))); //Saltare
        sinergie.add(new Sinergia("AB34", "AB29", new ModificatoreDTO(null, "AB29", 2, "+2", "per decifrare incantesimi su pergamene", TipoModificatore.VALORE, true, "Sinergia Usare Oggetti Magici", null))); //Sapienza Magica
        sinergie.add(new Sinergia("AB29", "AB34", new ModificatoreDTO(null, "AB34", 2, "+2", "attivare/decifrare pergamene", TipoModificatore.VALORE, true, "Sinergia Sapienza Magica", null))); //Usare Oggetti Magici
        sinergie.add(new Sinergia("AB33", "AB30", new ModificatoreDTO(null, "AB30", 2, "+2", "su corde", TipoModificatore.VALORE, true, "Sinergia Usare Corde", null))); //Arrampicare
        sinergie.add(new Sinergia("AB33", "AB4", new ModificatoreDTO(null, "AB4", 2, "+2", "per liberarsi da corde", TipoModificatore.VALORE, true, "Sinergia Usare Corde", null))); //Artista della fuga
        sinergie.add(new Sinergia("AB8", "AB32", new ModificatoreDTO(null, "AB32", 2, "+2", "quando segui tracce", TipoModificatore.VALORE, true, "Sinergia Cercare", null))); //Sopravvivenza
        return sinergie;
    }


}