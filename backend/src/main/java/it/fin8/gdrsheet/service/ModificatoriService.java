package it.fin8.gdrsheet.service;

import it.fin8.gdrsheet.config.Constants;
import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.def.TipoModificatore;
import it.fin8.gdrsheet.def.TipoStat;
import it.fin8.gdrsheet.dto.*;
import it.fin8.gdrsheet.entity.*;
import it.fin8.gdrsheet.repository.ItemRepository;
import it.fin8.gdrsheet.repository.PersonaggioRepository;
import it.fin8.gdrsheet.repository.StatRepository;
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

    @Autowired
    private DiceRoller roll;

    @Autowired
    private StatRepository statRepository;

    // ==================== CALCOLA* (metodi principali) ====================

    /**
     * Calcola una caratteristica (FOR/DES/COS/INT/SAG/CAR): somma i modificatori (VALORE, MOD
     * raddoppiato, BASE, Temporaneo, taglia per COS), poi applica l'eventuale fattore ×/÷
     * combinato (vedi estraiModificatoreMoltiplicaDividi) e deriva valore/modificatore/permanente.
     */
    CaratteristicaDTO calcolaCaratteristica(
            StatValue stat,
            List<ModificatoreDTO> modsDto,
            VariabiliDTO variabili
    ) {

        List<ModificatoreDTO> modificatoriAttivi = new ArrayList<>();
        aggiungiTemporaneo(modificatoriAttivi, stat);
        aggiungiModificatoreBase(modificatoriAttivi, modsDto);
        aggiungiModificatoreTagliaCOS(modificatoriAttivi, stat, variabili);
        aggiungiModificatoriValore(modificatoriAttivi, modsDto);
        aggiungiModificatoriValoreDaMod(modificatoriAttivi, modsDto);

        risolviFormule(modificatoriAttivi, variabili.mappa());

        int valore = modificatoriAttivi.stream()
                .filter(x -> x.getNota() == null)
                .mapToInt(ModificatoreDTO::getValore)
                .sum();

        valore = aggiungiMoltiplicatoreDivisore(modificatoriAttivi, stat.getStat().getId(), modsDto, variabili, valore);

        int valorePermanente = modificatoriAttivi.stream()
                .filter(x -> x.getNota() == null && Boolean.TRUE.equals(x.getSempreAttivo())
                        && !TipoModificatore.MOLTIPLICA.equals(x.getTipo())
                        && !TipoModificatore.DIVIDI.equals(x.getTipo()))
                .mapToInt(ModificatoreDTO::getValore)
                .sum();

        return new CaratteristicaDTO(
                stat.getStat().getId(),
                stat.getStat().getLabel(),
                valore,
                valoreToMod(valore),
                valoreToMod(valorePermanente),
                modificatoriAttivi
        );
    }

    /**
     * Calcola un tiro salvezza (Tempra/Riflessi/Volontà): caratteristica base (con eventuale
     * override CAMBIA_CARATTERISTICA specifico o globale) più i modificatori propri e Temporaneo.
     */
    TiroSalvezzaDTO calcoloTiroSalvezza(
            StatValue stat,
            List<ModificatoreDTO> modsDto,
            VariabiliDTO variabili,
            List<CaratteristicaDTO> carList,
            List<ModificatoreDTO> cambiaGlobali
    ) {
        List<ModificatoreDTO> modificatoriAttivi = new ArrayList<>(modsDto);
        aggiungiCambiaGlobali(modificatoriAttivi, cambiaGlobali);

        String modStatId = stat.getMod() != null ? stat.getMod().getId() : null;
        String cambia = risolviCambiaCaratteristica(modificatoriAttivi, stat.getStat().getId(), Constants.STAT_TUTTI_TS);
        if (cambia != null) modStatId = cambia;

        aggiungiCaratteristicaBase(modificatoriAttivi, stat, carList, modStatId);
        aggiungiTemporaneo(modificatoriAttivi, stat);

        risolviFormule(modificatoriAttivi, variabili.mappa());

        int modificatore = modificatoriAttivi.stream()
                .filter(x -> x.getNota() == null && !TipoModificatore.CAMBIA_CARATTERISTICA.equals(x.getTipo()))
                .mapToInt(ModificatoreDTO::getValore)
                .sum();

        return new TiroSalvezzaDTO(
                stat.getStat().getId(),
                stat.getStat().getLabel(),
                modificatore,
                modificatoriAttivi
        );
    }

    /**
     * Calcola un'abilità: caratteristica base (con eventuale override CAMBIA_CARATTERISTICA
     * specifico o globale) più i bonus dai modificatori e i gradi (di classe pieni, cross-class
     * dimezzati, o "maxati" se il personaggio ha un rank forzato al massimo).
     */
    AbilitaDTO calcolaAbilita(
            StatValue stat,
            List<ModificatoreDTO> modsDto,
            List<RankDTO> ranksDto,
            List<CaratteristicaDTO> carList,
            List<ModificatoreDTO> cambiaGlobali,
            VariabiliDTO variabili
    ) {
        List<ModificatoreDTO> mods = new ArrayList<>(modsDto);
        aggiungiCambiaGlobali(mods, cambiaGlobali);

        String modStatId = stat.getMod() != null ? stat.getMod().getId() : null;
        String cambia = risolviCambiaCaratteristica(mods, stat.getStat().getId(), Constants.STAT_TUTTE_ABILITA);
        if (cambia != null) modStatId = cambia;

        CaratteristicaDTO cBase = trovaCaratteristicaBase(carList, modStatId);
        int modBase = cBase != null ? cBase.getModificatore() : 0;

        aggiungiFormulaAbilita(mods, stat);
        risolviFormule(mods, variabili.mappa());
        int bonusVal = mods.stream()
                .filter(x -> x.getNota() == null && !TipoModificatore.CAMBIA_CARATTERISTICA.equals(x.getTipo()))
                .mapToInt(ModificatoreDTO::getValore)
                .sum();

        RankTotali rank = calcolaRankTotali(ranksDto);
        int total = modBase + bonusVal + rank.bonusRank();

        boolean negata = mods.stream().anyMatch(m -> TipoModificatore.NEGA.equals(m.getTipo()));
        boolean sbloccata = mods.stream().anyMatch(m -> TipoModificatore.SBLOCCA.equals(m.getTipo()));
        AbilitaDTO dto = new AbilitaDTO(stat, total, mods, rank.valoreRank(), rank.bonusRank(), ranksDto, cBase);
        if (negata) dto.setNegata(true);
        if (sbloccata) dto.setSbloccata(true);
        return dto;
    }

    /**
     * Calcola CA/CAC/CAS: bonus di taglia, caratteristica base, e i bonus CA
     * (schivare/armatura/naturale/scudo/magici, ciascuno preso al massimo per tipo) — ognuna delle
     * tre stat ne usa un sottoinsieme diverso (vedi aggiungiModificatoriClasseArmatura).
     */
    ClasseArmaturaDTO calcolaClasseArmatura(StatValue stat, List<ModificatoreDTO> modsDto, List<ModificatoreDTO> modsCADto,
                                            List<CaratteristicaDTO> carList,
                                            VariabiliDTO variabili) {
        // modsCADto è SEMPRE la lista grezza della stat "CA" (modsDtoByStat.get("CA")), la stessa
        // istanza riusata per le tre chiamate CA/CAC/CAS: risolviFormule rimuove le entry con
        // formula non valutabile, quindi va applicata a una COPIA, non alla lista condivisa
        // (altrimenti la prima chiamata la accorcerebbe anche per le altre due).
        risolviFormule(modsDto, variabili.mappa());
        List<ModificatoreDTO> modsCADtoRisolti = risolviCopia(modsCADto, variabili);
        int taglia = variabili.getInt(Constants.VARIABILE_TAGLIA);

        BonusCA bonus = estraiBonusCA(modsCADtoRisolti);
        List<ModificatoreDTO> valore = modsDto.stream().filter(x -> x.getNota() == null && (TipoModificatore.VALORE.equals(x.getTipo()) || TipoModificatore.CA_DEVIAZIONE.equals(x.getTipo()))).toList();
        List<ModificatoreDTO> valoreCA = modsCADtoRisolti.stream().filter(x -> x.getNota() == null && (TipoModificatore.VALORE.equals(x.getTipo()) || TipoModificatore.CA_DEVIAZIONE.equals(x.getTipo()))).toList();

        List<ModificatoreDTO> modificatoriAttivi = new ArrayList<>(valore);
        aggiungiTagliaCA(modificatoriAttivi, stat, taglia);
        aggiungiTemporaneo(modificatoriAttivi, stat);
        ModificatoreDTO baseMod = baseModDaCaratteristica(stat, carList);
        aggiungiModificatoriClasseArmatura(modificatoriAttivi, stat, baseMod, bonus, valoreCA);

        // Niente risolviFormule qui: ogni entry di modificatoriAttivi arriva già risolta (da
        // modsDto/modsCADtoRisolti sopra, o è un valore letterale come Taglia/baseMod/Temporaneo).
        int modificatore = sommaValori(modificatoriAttivi);

        return new ClasseArmaturaDTO(
                stat.getStat().getId(), stat.getStat().getLabel(),
                modificatore, modificatoriAttivi
        );
    }

    /**
     * Calcola BAB/LTT (Lotta)/GTT/MSC: BAB è il bonus attacco base grezzo; le altre tre aggiungono
     * il BAB, la caratteristica base e un bonus di taglia (lineare per LTT, tabella non lineare
     * per GTT/MSC, come CA/CAC/CAS).
     */
    BonusAttaccoDTO calcolaBonusAttacco(StatValue stat, List<ModificatoreDTO> modsDto, List<ModificatoreDTO> modsBABDto, List<CaratteristicaDTO> carList, VariabiliDTO variabili) {
        int taglia = variabili.getInt(Constants.VARIABILE_TAGLIA);
        risolviFormule(modsDto, variabili.mappa());

        List<ModificatoreDTO> valore = modsDto.stream().filter(x -> x.getNota() == null && x.getTipo() == TipoModificatore.VALORE).toList();
        List<ModificatoreDTO> valoreBAB = modsBABDto.stream().filter(x -> x.getNota() == null && x.getTipo() == TipoModificatore.VALORE).toList();
        int modificatoreBAB = valoreBAB.stream().mapToInt(ModificatoreDTO::getValore).sum();

        List<ModificatoreDTO> modificatoriAttivi = new ArrayList<>(valore);
        aggiungiTemporaneo(modificatoriAttivi, stat);
        ModificatoreDTO baseMod = baseModDaCaratteristica(stat, carList);

        String id = stat.getStat().getId();
        int modificatore = switch (id) {
            case "BAB" -> sommaValori(modificatoriAttivi);
            case "LTT" -> aggiungiTagliaEValoreBAB(modificatoriAttivi, stat, valoreBAB, baseMod, taglia, true);
            case "GTT", "MSC" -> aggiungiTagliaEValoreBAB(modificatoriAttivi, stat, valoreBAB, baseMod, taglia, false);
            default -> 0;
        };

        return new BonusAttaccoDTO(
                id, stat.getStat().getLabel(),
                modificatore, computeIterativeAttacks(modificatoreBAB, modificatore), modificatoriAttivi
        );
    }

    /**
     * Calcola un contatore generico (es. PF): la formula propria della stat (es. "10+@LVL"), la
     * caratteristica base, e (solo per PF) il malus da livelli maledetti.
     */
    public ContatoreDTO calcolaContatore(StatValue stat,
                                         List<ModificatoreDTO> modsDto,
                                         List<CaratteristicaDTO> carList,
                                         DadiVitaDTO dadiVita,
                                         List<Item> livelloItems,
                                         VariabiliDTO variabili) {
        espandiIdInFormule(modsDto);
        risolviFormule(modsDto, variabili.mappa());
        assicuraValoreNumerico(stat);

        List<ModificatoreDTO> modificatoriAttivi = new ArrayList<>(modsDto.stream().toList());
        aggiungiModCaratteristicaContatore(modificatoriAttivi, stat, carList);
        aggiungiBaseFormulaContatore(modificatoriAttivi, stat, dadiVita, variabili);
        aggiungiMalusPF(modificatoriAttivi, stat, livelloItems, dadiVita);

        int total = modificatoriAttivi.stream()
                .filter(x -> x.getNota() == null)
                .mapToInt(ModificatoreDTO::getValore)
                .sum();

        return new ContatoreDTO(
                stat.getStat().getId(),
                stat.getStat().getLabel(),
                Integer.parseInt(stat.getValore()),
                total,
                modificatoriAttivi
        );
    }

    /**
     * Calcola un attributo generico: VALORE/MOD/PERCENTUALE/MOLTIPLICA/DIVIDI applicati in
     * sequenza (vedi calcolaValoreAttributo) — un meccanismo più semplice e indipendente da quello
     * combinato usato in calcolaCaratteristica.
     */
    AttributoDTO calcolaAttributo(
            StatValue stat,
            List<ModificatoreDTO> modsDto,
            List<CaratteristicaDTO> carList,
            VariabiliDTO variabili
    ) {
        espandiIdInFormule(modsDto);
        risolviFormule(modsDto, variabili.mappa());

        List<ModificatoreDTO> modificatoriAttivi = new ArrayList<>();
        aggiungiTemporaneo(modificatoriAttivi, stat);
        aggiungiModificatoriAttributo(modificatoriAttivi, modsDto);
        ModificatoreDTO baseMod = baseModDaCaratteristica(stat, carList);
        if (baseMod != null) {
            modificatoriAttivi.add(baseMod);
        }

        int modificatore = calcolaValoreAttributo(modsDto, modificatoriAttivi);
        Integer percentuale = calcolaPercentualeAttributo(modsDto);

        return new AttributoDTO(
                stat.getStat().getId(), stat.getStat().getLabel(),
                modificatore, percentuale, modificatoriAttivi
        );
    }

    /**
     * Calcola i dadi vita totali: un dado per livello di classe (esclusi quelli maledetti, gestiti
     * a parte in calcolaContatore) più eventuali dadi vita extra da oggetti/buff.
     */
    DadiVitaDTO calcolaDadiVita(
            StatValue stat,
            List<ModificatoreDTO> modsDto,
            List<CaratteristicaDTO> carList,
            List<Item> livelloItems,
            VariabiliDTO variabili
    ) {
        risolviFormule(modsDto, variabili.mappa());

        List<ModificatoreDTO> modificatoriAttivi = new ArrayList<>();
        List<ModificatoreDTO> modPF = new ArrayList<>();
        aggiungiTemporaneo(modificatoriAttivi, stat);
        aggiungiDadiVitaLivelli(modificatoriAttivi, stat, livelloItems);
        aggiungiDadiVitaExtra(modificatoriAttivi, modPF, stat, modsDto);

        DiceSummary dice = utilService.combineDice(modificatoriAttivi, ModificatoreDTO::getFormula);

        ModificatoreDTO baseMod = baseModDaCaratteristica(stat, carList);
        if (baseMod != null) {
            modificatoriAttivi.add(baseMod);
        }

        return new DadiVitaDTO(
                stat.getStat().getId(), stat.getStat().getLabel(), dice.getTotalN(), dice.getCombined(), modificatoriAttivi, modPF
        );
    }

    // ==================== API pubblica non "calcola*" ====================

    /** Applica alla label $V_ (contatore) tutte le label $M_ (modificatori) che la referenziano, in ordine. */
    public ContatoreItemDTO calcolaContatoreItem(ItemLabel count, List<ItemLabel> modifiers) {

        String refVariabile = parseVariableFromLabel(count);
        try {
            for (ItemLabel modifier : modifiers) {
                String modVariable = parseVariableToModFromLabel(modifier);
                if (refVariabile.equals(modVariable)) {
                    applyModifierFromLabel(count, modifier);
                }
            }
        } catch (Exception e) {
            return new ContatoreItemDTO(refVariabile, 0);
        }


        return new ContatoreItemDTO(
                refVariabile, Integer.parseInt(count.getValore())
        );
    }

    /** Applica un singolo modificatore ($M_) a una variabile ($V_): +N/-N/=N sul suo valore. */
    public static void applyModifierFromLabel(ItemLabel var, ItemLabel mod) {
        if (var == null || mod == null) return;

        char op = mod.getValore().charAt(0);
        String operandStr = mod.getValore().substring(1).trim();
        int operand;

        if (op == '+' || op == '-' || op == '=') {
            operand = operandStr.isEmpty() ? 1 : Integer.parseInt(operandStr);
            switch (op) {
                case '+':
                    var.setValore(String.valueOf(Integer.parseInt(var.getValore()) + operand));
                    break;
                case '-':
                    var.setValore(String.valueOf(Integer.parseInt(var.getValore()) - operand));
                    break;
                case '=':
                    var.setValore(String.valueOf(operand));
                    break;
            }
        }
    }

    /** Risolve una label $M_... nell'id di variabile che modifica, con eventuale navigazione P/C
     *  (padre/figlio) verso l'item che porta la variabile bersaglio. */
    public static String parseVariableToModFromLabel(ItemLabel mod) {
        String label = mod.getLabel();
        if (!label.startsWith("$M_")) return null;
        Item item = mod.getItem();

        String pattern = label.substring(3); // tutto dopo $M_
        String[] parts = pattern.split("_");

        // Tutte le parti tranne l'ultima sono navigazioni, l'ultima è la variabile.
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == parts.length - 1) {
                // ultima parte = variabile
                return item.getId() + "_" + part;
            } else if ("P".equals(part)) {
                item = item.getFirstParent(null);
            } else if ("C".equals(part)) {
                item = item.getFirstChild(null);
            }
        }
        return null;
    }

    /** Id della variabile ($V_...) definita su questo item, o null se la label non è una variabile. */
    public static String parseVariableFromLabel(ItemLabel var) {
        String label = var.getLabel();
        if (!label.startsWith("$V_")) return null;
        Item item = var.getItem();
        String nomeVar = label.substring(3);
        return item.getId() + "_" + nomeVar;
    }

    /**
     * Classi uniche derivate dai livelli attivi (non disabilitati) del personaggio.
     * È la parte costosa di {@link #getAbilitaClasse(Integer, Integer, Integer)} (accede al DB):
     * dato che non dipende dal livello richiesto (il filtro per livello è disattivato, quindi il
     * risultato è identico per ogni livello dello stesso personaggio), va calcolata UNA SOLA VOLTA
     * per personaggio e riusata per ciascun livello, invece di essere rifatta identica ad ogni
     * iterazione. Il fetch delle classi è in batch (un'unica query IN) invece di N query singole.
     */
    public List<Item> getClassiUnicheDaLivelli(Integer idPersonaggio) {
        Personaggio personaggio = personaggioRepository.findById(idPersonaggio)
                .orElseThrow(() -> new RuntimeException("Personaggio non trovato"));

        List<Item> livelli = personaggio.getItems().stream()
                .filter(x -> x.getTipo().equals(TipoItem.LIVELLO))
                .filter(x -> {
                    String dis = x.getLabel(Constants.ITEM_LABEL_DISABILITATO);
                    return dis == null || Objects.equals(dis, Constants.ITEM_LABEL_DISABILITATO_VALORE_FALSE);
                })
                .toList();
        if (livelli.isEmpty()) return List.of();

        List<Integer> classeIds = livelli.stream()
                .map(x -> x.getLabel(Constants.ITEM_LABEL_CLASSE))
                .filter(Objects::nonNull)
                .distinct()
                .map(Integer::parseInt)
                .toList();
        if (classeIds.isEmpty()) return List.of();

        Map<Integer, Item> perId = itemRepository.findItemsByIds(classeIds).stream()
                .collect(Collectors.toMap(Item::getId, x -> x, (a, b) -> a));
        // preserva l'ordine originale (prima occorrenza nei livelli), scartando eventuali id mancanti
        return classeIds.stream().map(perId::get).filter(Objects::nonNull).toList();
    }

    /**
     * Placeholder generico di famiglia -> prefisso: se listato come abilità di classe, vale per
     * TUTTI i "figli" della famiglia (es. AR00 di classe => Artigianato: Armi, Artigianato:
     * Armature, ecc. sono tutte di classe), non solo per il placeholder stesso.
     */
    public static final Map<String, String> FAMIGLIA_GENERICA = Map.of(
            "AR00", "AR",
            "CO00", "CO",
            "IN00", "IN",
            "AB00", "AB"
    );

    /**
     * Id del placeholder di famiglia (AB00/AR00/CO00/IN00) che, come bersaglio di un
     * {@code Modificatore}, si applica a TUTTE le abilità con quel prefisso. Es.: "AB3" -> "AB00",
     * "AR01" -> "AR00". Ritorna {@code null} se l'id non corrisponde a nessuna famiglia nota.
     */
    public static String placeholderPerAbilita(String statId) {
        if (statId == null) return null;
        for (Map.Entry<String, String> e : FAMIGLIA_GENERICA.entrySet()) {
            String placeholder = e.getKey();
            String prefisso = e.getValue();
            if (!statId.equals(placeholder) && statId.startsWith(prefisso)) {
                return placeholder;
            }
        }
        return null;
    }

    /**
     * Indice abilità-di-classe per {@code idClasse}, a partire dalle classi già calcolate da
     * {@link #getClassiUnicheDaLivelli(Integer)}. Economico (nessun accesso al DB) nel caso comune:
     * la query per i "figli" di una famiglia (AR/CO/IN) viene fatta al più una volta per famiglia
     * e SOLO se una classe la referenzia tramite il placeholder generico (AR00/CO00/IN00), può
     * essere richiamato per ogni livello del personaggio riusando lo stesso {@code classiUniche}.
     */
    public List<AbilitaClasseDTO> getAbilitaClasse(List<Item> classiUniche, Integer idClasse) {
        Map<String, AbilitaClasseDTO> index = new LinkedHashMap<>();
        Map<String, List<String>> membriFamiglia = new HashMap<>();
        for (Item cl : classiUniche) {
            String abClasse = cl.getLabel(Constants.ITEM_LABEL_ABILITA_CLASSE);
            if (abClasse == null || abClasse.isBlank()) continue;

            Arrays.stream(abClasse.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(tok -> {
                        boolean all = tok.contains("!");
                        // "?" (componibile con "!"): questo token da solo non deve alzare il limite
                        // gradi (resta spendibile come cross-class) — ma se la stessa abilità è
                        // comunque di classe per un altro motivo (altro token/altra classe senza
                        // "?"), quell'altra fonte imposta comunque dto.setAll/setDiClasse più sotto
                        // nello stesso indice, quindi il limite pieno si applica lo stesso.
                        boolean escludiCap = tok.contains("?");
                        boolean classeRichiesta = cl.getId().equals(idClasse);
                        String idAb = tok.replace("!", "").replace("?", "").trim().toUpperCase();

                        String prefissoFamiglia = FAMIGLIA_GENERICA.get(idAb);
                        List<String> idsDaMarcare = prefissoFamiglia == null
                                ? List.of(idAb)
                                : membriFamiglia.computeIfAbsent(prefissoFamiglia,
                                p -> statRepository.findAllByTipoAndIdStartingWith(TipoStat.AB, p)
                                        .stream()
                                        // le abilità non "rankable" non salgono di livello: non vanno
                                        // considerate parte di "tutte le X" (placeholder di famiglia)
                                        .filter(s -> !Boolean.FALSE.equals(s.getRankable()))
                                        .map(Stat::getId).toList());
                        if (idsDaMarcare.isEmpty())
                            idsDaMarcare = List.of(idAb); // nessun figlio a catalogo: marca almeno il placeholder

                        for (String id : idsDaMarcare) {
                            AbilitaClasseDTO dto = index.computeIfAbsent(
                                    id,
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
                        }
                    });
        }
        return new ArrayList<>(index.values());
    }

    /** Firma storica: calcola le classiUniche e delega. Da preferire {@link #getAbilitaClasse(List, Integer)}
     *  quando si itera su più livelli dello stesso personaggio (vedi {@link #getClassiUnicheDaLivelli(Integer)}). */
    public List<AbilitaClasseDTO> getAbilitaClasse(Integer idPersonaggio, Integer livello, Integer idClasse) {
        return getAbilitaClasse(getClassiUnicheDaLivelli(idPersonaggio), idClasse);
    }

    /**
     * Applica le sinergie tra abilità (5+ gradi in un'abilità sorgente danno un bonus fisso su
     * un'abilità bersaglio, vedi listaSinergie): per ogni abilità con una sinergia attiva, aggiunge
     * il modificatore di sinergia e ricalcola l'abilità, sostituendola nella lista.
     */
    public void applicaSinergie(DatiPersonaggioDTO dp, List<CaratteristicaDTO> cars) {
        List<AbilitaDTO> abilitaList = dp.getAbilita();

        for (int i = 0; i < abilitaList.size(); i++) {
            AbilitaDTO ab = abilitaList.get(i);

            AbilitaDTO finalAb = ab;
            List<Sinergia> sinergie = listaSinergie().stream()
                    .filter(x -> x.getT().equals(finalAb.getAbilita().getId()))
                    .toList();

            for (Sinergia s : sinergie) {
                AbilitaDTO abilitaSource = abilitaList.stream()
                        .filter(x -> x.getAbilita().getId().equals(s.getS()))
                        .findFirst()
                        .orElse(null);

                if (abilitaSource != null && abilitaSource.getRank().getModificatore() >= 5) {
                    Integer rankVal = abilitaSource.getRank().getModificatore();

                    if (ab.getAbilita().getModificatori() == null || ab.getAbilita().getModificatori().isEmpty()) {
                        ab.getAbilita().setModificatori(new ArrayList<>());
                    }

                    ModificatoreDTO mod = s.getM();
                    int moltiplicatore = rankVal / 5;
                    String formula = mod.getFormula() + "*" + moltiplicatore;

                    mod.setFormula(calcoloService.calcola(formula, Collections.emptyList()));
                    mod.setValore(Integer.parseInt(mod.getFormula()));

                    ab.getAbilita().getModificatori().add(mod);

                    // Calcola la nuova abilità e sostituiscila nella lista.
                    // Il CAMBIA_CARATTERISTICA globale è già nei modificatori dell'abilità: emptyList.
                    AbilitaDTO nuovaAbilita = calcolaAbilita(
                            ab.getStat(),
                            ab.getAbilita().getModificatori(),
                            ab.getRank().getRanks(),
                            cars,
                            Collections.emptyList(),
                            costruisciVariabili(List.of(), cars)
                    );

                    abilitaList.set(i, nuovaAbilita);
                    ab = nuovaAbilita; // aggiorna riferimento locale per ulteriori possibili sinergie
                }
            }
        }
    }

    /** Tabella statica delle sinergie tra abilità SRD 3.5 (abilità sorgente → abilità bersaglio → bonus). */
    private List<Sinergia> listaSinergie() {
        List<Sinergia> sinergie = new ArrayList<>();
        // TODO: Gestire Artigianato e Valutare diversi e aggiungere Sinergie
        sinergie.add(new Sinergia("AB26", "AB12", new ModificatoreDTO(null, "AB12", 2, "+2", null, TipoModificatore.VALORE, true, "Sinergia Raggirare", null, null))); //Diplomazia
        sinergie.add(new Sinergia("AB26", "AB17", new ModificatoreDTO(null, "AB17", 2, "+2", null, TipoModificatore.VALORE, true, "Sinergia Raggirare", null, null))); //Intimidire
        sinergie.add(new Sinergia("AB26", "AB6", new ModificatoreDTO(null, "AB6", 2, "+2", "Quando reciti un ruolo", TipoModificatore.VALORE, true, "Sinergia Raggirare", null, null))); //Camuffare
        sinergie.add(new Sinergia("AB26", "AB27", new ModificatoreDTO(null, "AB27", 2, "+2", null, TipoModificatore.VALORE, true, "Sinergia Raggirare", null, null))); //Rapidità di Mano
        sinergie.add(new Sinergia("AB11", "AB34", new ModificatoreDTO(null, "AB34", 2, "+2", "Quando usi Pergamene", TipoModificatore.VALORE, true, "Sinergia Decifrare Scritture", null, null))); //Utilizzare Oggetti Magici
        sinergie.add(new Sinergia("AB4", "AB33", new ModificatoreDTO(null, "AB33", 2, "+2", "legature/vincoli", TipoModificatore.VALORE, true, "Sinergia Artista della fuga", null, null))); //Usare Corde
        sinergie.add(new Sinergia("AB2", "AB7", new ModificatoreDTO(null, "AB7", 2, "+2", null, TipoModificatore.VALORE, true, "Sinergia Addestrare Animali", null, null))); //Cavalcare
        sinergie.add(new Sinergia("AB2", "AB37", new ModificatoreDTO(null, "AB37", 2, "+2", null, TipoModificatore.VALORE, true, "Sinergia Addestrare Animali", null, null))); //Empatia Selvatica
        sinergie.add(new Sinergia("AB28", "AB1", new ModificatoreDTO(null, "AB1", 2, "+2", null, TipoModificatore.VALORE, true, "Sinergia Saltare", null, null))); //Acrobazia
        sinergie.add(new Sinergia("CO06", "AB29", new ModificatoreDTO(null, "AB29", 2, "+2", null, TipoModificatore.VALORE, true, "Sinergia Conoscenze Arcane", null, null))); //Sapienza Magica
        sinergie.add(new Sinergia("CO09", "AB8", new ModificatoreDTO(null, "AB8", 2, "+2", "porte segrete/scomparti simili", TipoModificatore.VALORE, true, "Sinergia Conoscenze (architettura e ingegneria)", null, null))); //Cercare
        sinergie.add(new Sinergia("CO01", "AB32", new ModificatoreDTO(null, "AB32", 2, "+2", "Sottoterra", TipoModificatore.VALORE, true, "Sinergia Conoscenze (dungeon)", null, null))); //Sopravvivenza
        sinergie.add(new Sinergia("CO04", "AB32", new ModificatoreDTO(null, "AB32", 2, "+2", "orientarsi/evitare pericoli", TipoModificatore.VALORE, true, "Sinergia Conoscenze (geografia)", null, null))); //Sopravvivenza
        sinergie.add(new Sinergia("CO11", "C012", new ModificatoreDTO(null, "C012", 2, "+2", null, TipoModificatore.VALORE, true, "Sinergia Conoscenze (Storia)", null, null))); //Conoscenze Bardiche
        sinergie.add(new Sinergia("CO07", "AB25", new ModificatoreDTO(null, "AB25", 2, "+2", null, TipoModificatore.VALORE, true, "Sinergia Conoscenze (locale)", null, null))); //Raccogliere Informazioni
        sinergie.add(new Sinergia("CO10", "AB32", new ModificatoreDTO(null, "AB32", 2, "+2", "ambienti naturali all’aperto", TipoModificatore.VALORE, true, "Sinergia Conoscenze (natura)", null, null))); //Sopravvivenza
        sinergie.add(new Sinergia("CO02", "AB12", new ModificatoreDTO(null, "AB12", 2, "+2", null, TipoModificatore.VALORE, true, "Sinergia Conoscenze (nobiltà e regalità)", null, null))); //Diplomazia
        sinergie.add(new Sinergia("CO08", "CO08", new ModificatoreDTO(null, "B", 2, "+2", "Prove per scacciare non morti", TipoModificatore.VALORE, true, "?? Sinergia Conoscenze Religioni", null, null)));
        sinergie.add(new Sinergia("CO05", "AB32", new ModificatoreDTO(null, "AB32", 2, "+2", "su altri piani", TipoModificatore.VALORE, true, "Sinergia Conoscenze (i piani) ", null, null))); //Sopravvivenza
        sinergie.add(new Sinergia("AB23", "AB12", new ModificatoreDTO(null, "AB12", 2, "+2", null, TipoModificatore.VALORE, true, "Sinergia Percepire Intenzioni", null, null))); //Diplomazia
        sinergie.add(new Sinergia("AB1", "AB14", new ModificatoreDTO(null, "AB14", 2, "+2", null, TipoModificatore.VALORE, true, "Sinergia Acrobazia", null, null))); //Equilibrio
        sinergie.add(new Sinergia("AB1", "AB28", new ModificatoreDTO(null, "AB28", 2, "+2", null, TipoModificatore.VALORE, true, "Sinergia Acrobazia", null, null))); //Saltare
        sinergie.add(new Sinergia("AB34", "AB29", new ModificatoreDTO(null, "AB29", 2, "+2", "per decifrare incantesimi su pergamene", TipoModificatore.VALORE, true, "Sinergia Usare Oggetti Magici", null, null))); //Sapienza Magica
        sinergie.add(new Sinergia("AB29", "AB34", new ModificatoreDTO(null, "AB34", 2, "+2", "attivare/decifrare pergamene", TipoModificatore.VALORE, true, "Sinergia Sapienza Magica", null, null))); //Usare Oggetti Magici
        sinergie.add(new Sinergia("AB33", "AB30", new ModificatoreDTO(null, "AB30", 2, "+2", "su corde", TipoModificatore.VALORE, true, "Sinergia Usare Corde", null, null))); //Arrampicare
        sinergie.add(new Sinergia("AB33", "AB4", new ModificatoreDTO(null, "AB4", 2, "+2", "per liberarsi da corde", TipoModificatore.VALORE, true, "Sinergia Usare Corde", null, null))); //Artista della fuga
        sinergie.add(new Sinergia("AB8", "AB32", new ModificatoreDTO(null, "AB32", 2, "+2", "quando segui tracce", TipoModificatore.VALORE, true, "Sinergia Cercare", null, null))); //Sopravvivenza
        return sinergie;
    }

    // ==================== Helper privati usati dai calcola* ====================

    /**
     * BASE: prendiMaxDTO ne tiene solo uno (i BASE si sovrascrivono a vicenda) — quello rimasto è
     * per definizione IL valore base della caratteristica, quindi sempre attivo anche se la
     * entity da cui viene non lo segnava esplicitamente (altrimenti valorePermanente lo
     * escluderebbe del tutto). Se non c'è un BASE (valori base non ancora impostati) non aggiunge
     * nulla, la caratteristica parte da 0.
     */
    private void aggiungiModificatoreBase(List<ModificatoreDTO> modificatoriAttivi, List<ModificatoreDTO> modsDto) {
        if (modsDto.isEmpty()) return;
        ModificatoreDTO baseDto = prendiMaxDTO(modsDto, TipoModificatore.BASE);
        if (baseDto == null) return;
        if (!Boolean.TRUE.equals(baseDto.getSempreAttivo())) baseDto.setSempreAttivo(true);
        modificatoriAttivi.add(baseDto);
    }

    /**
     * Malus/bonus di taglia: solo su COS, +4 per punto di differenza tra taglia attuale e base
     * (vedi Constants.VARIABILE_DIFFERENZA_TAGLIA, calcolata fuori una volta sola).
     */
    private void aggiungiModificatoreTagliaCOS(List<ModificatoreDTO> modificatoriAttivi, StatValue stat, VariabiliDTO variabili) {
        if (!stat.getStat().getId().equals("COS")) return;
        int differenzaTaglia = variabili.getInt(Constants.VARIABILE_DIFFERENZA_TAGLIA);
        if (differenzaTaglia != 0) {
            modificatoriAttivi.add(new ModificatoreDTO(null, stat.getStat().getId(), 4 * differenzaTaglia, null, null, TipoModificatore.VALORE, false, "Taglia", null, null));
        }
    }

    /**
     * Modificatori di tipo VALORE, aggiunti così come sono.
     */
    private void aggiungiModificatoriValore(List<ModificatoreDTO> modificatoriAttivi, List<ModificatoreDTO> modsDto) {
        modificatoriAttivi.addAll(modsDto.stream().filter(m -> TipoModificatore.VALORE.equals(m.getTipo())).toList());
    }

    /**
     * Modificatori di tipo MOD: valgono metà di VALORE → mutati a ×2 in-place (via peek) così
     * popup e somma mostrano il valore effettivo, senza una lista intermedia inutile.
     */
    private void aggiungiModificatoriValoreDaMod(List<ModificatoreDTO> modificatoriAttivi, List<ModificatoreDTO> modsDto) {
        modificatoriAttivi.addAll(modsDto.stream()
                .filter(m -> TipoModificatore.MOD.equals(m.getTipo()))
                .peek(m -> {
                    if (m.getValore() != null) m.setValore(m.getValore() * 2);
                    if (m.getFormula() != null && !m.getFormula().isBlank()) m.setFormula(m.getFormula().concat("*2"));
                })
                .toList());
    }

    /**
     * Estrae (vedi estraiModificatoreMoltiplicaDividi) e aggiunge il modificatore MOLTIPLICA/DIVIDI
     * combinato, se presente — il fattore netto ×/÷ moltiplica/divide direttamente il VALORE, non
     * il modificatore ((valore-10)/2), che resta una derivazione automatica fatta più avanti (vedi
     * valoreToMod). Ritorna il valore aggiornato con il delta del modificatore combinato.
     */
    private int aggiungiMoltiplicatoreDivisore(List<ModificatoreDTO> modificatoriAttivi, String statId, List<ModificatoreDTO> modsDto, VariabiliDTO variabili, int valore) {
        ModificatoreDTO moltiplicaDividi = estraiModificatoreMoltiplicaDividi(statId, modsDto, variabili, valore);
        if (moltiplicaDividi == null) return valore;
        modificatoriAttivi.add(moltiplicaDividi);
        return valore + moltiplicaDividi.getValore();
    }

    /**
     * Unisce cambiaGlobali a mods evitando duplicati per id: usato da calcoloTiroSalvezza (TS
     * globale) e calcolaAbilita (abilità globale) per rendere visibile l'override globale nel
     * popup e preservarlo nelle ricomputazioni (es. sinergie).
     */
    private void aggiungiCambiaGlobali(List<ModificatoreDTO> mods, List<ModificatoreDTO> cambiaGlobali) {
        if (cambiaGlobali == null) return;
        for (ModificatoreDTO g : cambiaGlobali) {
            if (mods.stream().noneMatch(m -> Objects.equals(m.getId(), g.getId()))) mods.add(g);
        }
    }

    /**
     * CAMBIA_CARATTERISTICA: id della caratteristica che sostituisce quella base di una stat/abilità
     * (la formula del modificatore contiene l'id bersaglio, es. "DES"). Priorità all'override
     * specifico su {@code statId}, poi a quello globale su {@code statGlobaleKey} (es.
     * Constants.STAT_TUTTI_TS o STAT_TUTTE_ABILITA). null se non c'è nessun override.
     */
    private String risolviCambiaCaratteristica(List<ModificatoreDTO> mods, String statId, String statGlobaleKey) {
        String specifico = mods.stream()
                .filter(m -> TipoModificatore.CAMBIA_CARATTERISTICA.equals(m.getTipo()) && statId.equals(m.getStatId()))
                .map(ModificatoreDTO::getFormula).filter(s -> s != null && !s.isBlank())
                .reduce((a, b) -> b).orElse(null);
        String globale = mods.stream()
                .filter(m -> TipoModificatore.CAMBIA_CARATTERISTICA.equals(m.getTipo()) && statGlobaleKey.equals(m.getStatId()))
                .map(ModificatoreDTO::getFormula).filter(s -> s != null && !s.isBlank())
                .reduce((a, b) -> b).orElse(null);
        return specifico != null ? specifico : globale;
    }

    /**
     * Aggiunge il modificatore "caratteristica base" del tiro salvezza (con eventuale override
     * CAMBIA_CARATTERISTICA già risolto in modStatId), se presente in carList.
     */
    private void aggiungiCaratteristicaBase(List<ModificatoreDTO> modificatoriAttivi, StatValue stat, List<CaratteristicaDTO> carList, String modStatId) {
        if (modStatId == null) return;
        carList.stream().filter(c -> modStatId.equals(c.getId())).findFirst()
                .ifPresent(c -> modificatoriAttivi.add(new ModificatoreDTO(
                        null, stat.getStat().getId(), c.getModificatore(), null, null,
                        TipoModificatore.VALORE, true, c.getLabel(), null, null)));
    }

    /**
     * La CaratteristicaDTO bersaglio di un'abilità (con eventuale override CAMBIA_CARATTERISTICA
     * già risolto in modStatId), o null se modStatId è null o non trovata in carList.
     */
    private CaratteristicaDTO trovaCaratteristicaBase(List<CaratteristicaDTO> carList, String modStatId) {
        if (modStatId == null) return null;
        return carList.stream().filter(c -> modStatId.equals(c.getId())).findFirst().orElse(null);
    }

    /**
     * Aggiunge a mods un modificatore "Formula" per la formula propria dell'abilità
     * (stat.getFormula()), con guard "già presente": quando calcolaAbilita viene richiamato una
     * seconda volta sullo stesso modsDto già calcolato (es. applicaSinergie ricalcola l'abilità
     * passando la sua stessa lista di modificatori finale), senza questo controllo la formula
     * verrebbe riaggiunta e il suo valore sommato due volte.
     */
    private void aggiungiFormulaAbilita(List<ModificatoreDTO> mods, StatValue stat) {
        boolean formulaGiaPresente = mods.stream()
                .anyMatch(m -> "Formula".equals(m.getItem()) && stat.getStat().getId().equals(m.getStatId()));
        if (!formulaGiaPresente && stat.getFormula() != null && !stat.getFormula().isBlank()) {
            try {
                mods.add(new ModificatoreDTO(null, stat.getStat().getId(), 0, stat.getFormula(), null, null, true, "Formula", null, null));
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Bonus rank di un'abilità (di classe pieno, cross-class dimezzato) e valore rank grezzo,
     * entrambi sostituiti dal maxato se presente (rank "al massimo", es. da talento/abilità).
     */
    private record RankTotali(int bonusRank, int valoreRank) {
    }

    /** Calcola i totali rank (bonus e valore grezzo) a partire dai RankDTO sempre attivi, vedi RankTotali. */
    private RankTotali calcolaRankTotali(List<RankDTO> ranksDto) {
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
        Integer maxatoRank = ranksDto.stream().map(RankDTO::getMaxato).filter(Objects::nonNull).findFirst().orElse(null);
        if (maxatoRank != null) return new RankTotali(maxatoRank, maxatoRank);
        return new RankTotali(bonusRank, valoreRank);
    }

    /**
     * Copia la lista e vi risolve le formule ($/@): usata quando la lista originale è condivisa
     * tra più chiamate (es. modsCADto tra CA/CAC/CAS) e risolviFormule non può rimuoverne le entry
     * in place senza accorciarla anche per le altre chiamate.
     */
    private List<ModificatoreDTO> risolviCopia(List<ModificatoreDTO> lista, VariabiliDTO variabili) {
        List<ModificatoreDTO> copia = new ArrayList<>(lista);
        risolviFormule(copia, variabili.mappa());
        return copia;
    }

    /**
     * I quattro bonus "prendi il massimo" di CA (schivare/armatura/naturale/scudo/magici),
     * ciascuno il modificatore più alto del proprio tipo tra i modificatori della stat "CA".
     */
    private record BonusCA(ModificatoreDTO schivare, ModificatoreDTO armor, ModificatoreDTO naturale,
                           ModificatoreDTO scudo, ModificatoreDTO magici) {
    }

    /** Estrae i cinque bonus CA (vedi BonusCA) dai modificatori della stat "CA". */
    private BonusCA estraiBonusCA(List<ModificatoreDTO> modsCADtoRisolti) {
        return new BonusCA(
                prendiMaxDTO(modsCADtoRisolti, TipoModificatore.CA_SCHIVARE),
                prendiMaxDTO(modsCADtoRisolti, TipoModificatore.CA_ARMOR),
                prendiMaxDTO(modsCADtoRisolti, TipoModificatore.CA_NATURALE),
                prendiMaxDTO(modsCADtoRisolti, TipoModificatore.CA_SHIELD),
                prendiMaxDTO(modsCADtoRisolti, TipoModificatore.CA_MAGIC)
        );
    }

    /**
     * Modificatore di taglia per CA/CAC/CAS (tabella non lineare, vedi SIZE_MOD_CA_ATTACCO).
     */
    private void aggiungiTagliaCA(List<ModificatoreDTO> modificatoriAttivi, StatValue stat, int taglia) {
        if (taglia != 0) {
            modificatoriAttivi.add(new ModificatoreDTO(null, stat.getStat().getId(), sizeModCaAttacco(taglia), "TAGLIA", null, null, true, "Taglia", null, null));
        }
    }

    /**
     * CA/CAC/CAS pescano un sottoinsieme diverso dei bonus CA (una debolezza già SRD: CAC esclude
     * naturale/scudo, CAS esclude base/schivare) — vedi CaratteristicaDTO per il significato di
     * ciascun bonus.
     */
    private void aggiungiModificatoriClasseArmatura(List<ModificatoreDTO> modificatoriAttivi, StatValue stat, ModificatoreDTO baseMod, BonusCA bonus, List<ModificatoreDTO> valoreCA) {
        String id = stat.getStat().getId();
        if (id.equals("CA")) {
            modificatoriAttivi.addAll(Stream.of(
                    baseMod, bonus.schivare(), bonus.armor(), bonus.naturale(), bonus.scudo(), bonus.magici()
            ).filter(Objects::nonNull).toList());
        }
        if (id.equals("CAC")) {
            modificatoriAttivi.addAll(valoreCA);
            modificatoriAttivi.addAll(Stream.of(
                    baseMod, bonus.schivare(), bonus.magici()
            ).filter(Objects::nonNull).toList());
        }
        if (id.equals("CAS")) {
            modificatoriAttivi.addAll(valoreCA);
            modificatoriAttivi.addAll(Stream.of(
                    bonus.armor(), bonus.naturale(), bonus.scudo(), bonus.magici()
            ).filter(Objects::nonNull).toList());
        }
    }

    /**
     * Somma i valore di una lista di modificatori già risolti (niente Optional: la lista non è mai null).
     */
    private int sommaValori(List<ModificatoreDTO> lista) {
        return lista.stream().mapToInt(ModificatoreDTO::getValore).sum();
    }

    /**
     * LTT/GTT/MSC condividono la stessa struttura (valoreBAB + baseMod + bonus di taglia), diversi
     * solo per la formula di taglia: LTT è lineare (4*taglia, come da regola di Lotta/Presa), GTT e
     * MSC usano la tabella non lineare (sizeModCaAttacco, come CA/CAC/CAS).
     */
    private int aggiungiTagliaEValoreBAB(List<ModificatoreDTO> modificatoriAttivi, StatValue stat, List<ModificatoreDTO> valoreBAB, ModificatoreDTO baseMod, int taglia, boolean tagliaLineare) {
        modificatoriAttivi.addAll(valoreBAB);
        if (baseMod != null) modificatoriAttivi.add(baseMod);
        if (taglia != 0) {
            int valoreTaglia = tagliaLineare ? 4 * taglia : sizeModCaAttacco(taglia);
            String formula = tagliaLineare ? "4*TAGLIA" : "TAGLIA";
            modificatoriAttivi.add(new ModificatoreDTO(null, stat.getStat().getId(), valoreTaglia, formula, null, null, true, "Taglia", null, null));
        }
        return sommaValori(modificatoriAttivi);
    }

    /**
     * Riporta stat.getValore() a "0" se non è un numero valido (guard difensiva usata da
     * calcolaContatore prima di fare Integer.parseInt su di esso più avanti).
     */
    private void assicuraValoreNumerico(StatValue stat) {
        try {
            Integer.parseInt(stat.getValore());
        } catch (Exception e) {
            stat.setValore("0");
        }
    }

    /**
     * Modificatore "caratteristica base" di un contatore (es. PF da COS), se stat.getMod() è
     * presente in carList.
     */
    private void aggiungiModCaratteristicaContatore(List<ModificatoreDTO> modificatoriAttivi, StatValue stat, List<CaratteristicaDTO> carList) {
        if (stat.getMod() == null) return;
        carList.stream()
                .filter(c -> c.getId().equals(stat.getMod().getId()))
                .findFirst().ifPresent(baseCar ->
                        modificatoriAttivi.add(new ModificatoreDTO(null, stat.getMod().getId(), baseCar.getModificatore(), null, null, null, true, stat.getMod().getLabel(), null, null)));
    }

    /**
     * Modificatore "BASE" di un contatore, dalla formula propria della stat (es. "10+@LVL" per
     * PF): @DV viene espanso prima al totale dadi vita, poi il resto è valutato normalmente.
     */
    private void aggiungiBaseFormulaContatore(List<ModificatoreDTO> modificatoriAttivi, StatValue stat, DadiVitaDTO dadiVita, VariabiliDTO variabili) {
        if (stat.getFormula() == null) return;
        String formula = stat.getFormula().replaceAll("@DV", String.valueOf(dadiVita.getTotale()));
        int formulaEvaluated = Integer.parseInt(calcoloService.calcola(formula, variabili.mappa()));
        modificatoriAttivi.add(new ModificatoreDTO(null, null, formulaEvaluated, formula, null, null, true, "BASE", null, null));
    }

    /**
     * Malus PF da livelli maledetti (solo per il contatore PF), più il pool di modificatori PF
     * già calcolato altrove sui dadi vita.
     */
    private void aggiungiMalusPF(List<ModificatoreDTO> modificatoriAttivi, StatValue stat, List<Item> livelloItems, DadiVitaDTO dadiVita) {
        if (!stat.getStat().getId().equals("PF")) return;
        modificatoriAttivi.addAll(livelloItems.stream().filter(x -> x.getLabel(Constants.ITEM_LABEL_MALEDIZIONE) != null).map(ModificatoriService::getMalusPF).toList());
        modificatoriAttivi.addAll(dadiVita.getModPF());
    }

    /**
     * Aggiunge tutti i tipi di modificatore di un attributo generico (VALORE sempre/situazionali,
     * MOD, PERCENTUALE, MOLTIPLICA/DIVIDI), così come sono — il calcolo vero e proprio (con MOD
     * raddoppiato e moltiplica/dividi applicati) è in calcolaValoreAttributo.
     */
    private void aggiungiModificatoriAttributo(List<ModificatoreDTO> modificatoriAttivi, List<ModificatoreDTO> modsDto) {
        // VALORE: sempre attivi (senza nota) e situazionali (con nota)
        modificatoriAttivi.addAll(modsDto.stream().filter(x -> x.getNota() == null && x.getTipo() == TipoModificatore.VALORE).toList());
        modificatoriAttivi.addAll(modsDto.stream().filter(x -> x.getNota() != null && x.getTipo() == TipoModificatore.VALORE).toList());
        // MOD: vale metà di VALORE — inclusi nel popup, contribuiscono ×2 al totale
        modificatoriAttivi.addAll(modsDto.stream().filter(x -> x.getTipo() == TipoModificatore.MOD).toList());
        // PERCENTUALE: calcolato separatamente, incluso nel popup
        modificatoriAttivi.addAll(modsDto.stream().filter(x -> x.getTipo() == TipoModificatore.PERCENTUALE).toList());
        // MOLTIPLICA / DIVIDI: post-processing, inclusi nel popup
        modificatoriAttivi.addAll(modsDto.stream().filter(x -> x.getTipo() == TipoModificatore.MOLTIPLICA || x.getTipo() == TipoModificatore.DIVIDI).toList());
    }

    /**
     * Somma base (VALORE normale, MOD raddoppiato; esclusi PERCENTUALE/MOLTIPLICA/DIVIDI), poi
     * applica in sequenza (ordine crescente di valore) i MOLTIPLICA e i DIVIDI — a differenza di
     * estraiModificatoreMoltiplicaDividi (usato da calcolaCaratteristica), qui non sono combinati
     * in un unico modificatore ridotto ai minimi termini: meccanismo più semplice, indipendente.
     */
    private int calcolaValoreAttributo(List<ModificatoreDTO> modsDto, List<ModificatoreDTO> modificatoriAttivi) {
        int modificatore = modificatoriAttivi.stream()
                .filter(x -> x.getNota() == null
                        && x.getTipo() != TipoModificatore.PERCENTUALE
                        && x.getTipo() != TipoModificatore.MOLTIPLICA
                        && x.getTipo() != TipoModificatore.DIVIDI)
                .mapToInt(m -> TipoModificatore.MOD.equals(m.getTipo()) ? m.getValore() * 2 : m.getValore())
                .sum();

        List<Integer> moltiplicatori = modsDto.stream()
                .filter(x -> x.getNota() == null && TipoModificatore.MOLTIPLICA.equals(x.getTipo()))
                .map(ModificatoreDTO::getValore)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        for (int m : moltiplicatori) modificatore *= m;

        List<Integer> divisori = modsDto.stream()
                .filter(x -> x.getNota() == null && TipoModificatore.DIVIDI.equals(x.getTipo()))
                .map(ModificatoreDTO::getValore)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        for (int d : divisori) if (d != 0) modificatore /= d;

        return modificatore;
    }

    /**
     * Somma dei modificatori PERCENTUALE, null se 0 (per non mostrarla nel popup).
     */
    private Integer calcolaPercentualeAttributo(List<ModificatoreDTO> modsDto) {
        int percentuale = modsDto.stream()
                .filter(x -> x.getNota() == null && x.getTipo() == TipoModificatore.PERCENTUALE)
                .mapToInt(ModificatoreDTO::getValore).sum();
        return percentuale == 0 ? null : percentuale;
    }

    /**
     * Un modificatore per livello di classe (Dado Vita di quel livello), esclusi i livelli
     * maledetti (gestiti a parte, vedi aggiungiMalusPF/getMalusPF).
     */
    private void aggiungiDadiVitaLivelli(List<ModificatoreDTO> modificatoriAttivi, StatValue stat, List<Item> livelloItems) {
        modificatoriAttivi.addAll(livelloItems.stream()
                .filter(x -> x.getLabel(Constants.ITEM_LABEL_MALEDIZIONE) == null)
                .map(x -> new ModificatoreDTO(null, stat.getStat().getId(), null, x.getLabel(Constants.ITEM_LABEL_DADI_VITA), null, TipoModificatore.VALORE, true, x.getNome(), null, TipoItem.LIVELLO))
                .toList());
    }

    /**
     * Modificatori extra ai dadi vita (oggetti/buff, non da livello): un modificatore normale in
     * modificatoriAttivi, più il suo corrispettivo tiro di dado (via DiceRoller) nel pool modPF.
     */
    private void aggiungiDadiVitaExtra(List<ModificatoreDTO> modificatoriAttivi, List<ModificatoreDTO> modPF, StatValue stat, List<ModificatoreDTO> modsDto) {
        modsDto.stream().filter(x -> !x.getTipoItem().equals(TipoItem.LIVELLO)).forEach(x -> {
            modificatoriAttivi.add(new ModificatoreDTO(null, stat.getStat().getLabel(), x.getValore(), x.getFormula(), null, null, true, x.getItem(), x.getItemId(), null));
            try {
                modPF.add(new ModificatoreDTO(null, "PF", DiceRoller.roll(x.getFormula(), true), x.getFormula(), null, TipoModificatore.VALORE, true, x.getItem(), x.getItemId(), null));
            } catch (Exception ignored) {
            }
        });
    }

    /** Il modificatore con valore più alto di un certo tipo; per BASE, un eventuale FORZATO vince
     *  sempre (prendendo il minimo tra i forzati, non il massimo). */
    private ModificatoreDTO prendiMaxDTO(List<ModificatoreDTO> mods, TipoModificatore tipo) {
        if (tipo == TipoModificatore.BASE) {
            ModificatoreDTO modForzato = mods.stream()
                    .filter(m -> TipoModificatore.FORZATO.equals(m.getTipo()))
                    .min(Comparator.comparing(ModificatoreDTO::getValore))
                    .orElse(null);
            if (modForzato != null) {
                modForzato.setTipo(TipoModificatore.BASE);
                return modForzato;
            }
        }
        return mods.stream()
                .filter(m -> tipo.equals(m.getTipo()))
                .max(Comparator.comparing(ModificatoreDTO::getValore))
                .orElse(null);
    }

    // Modificatore di taglia per CA e attacco (mischia/gittata), tabella SRD indicizzata da taglia+4
    // (taglia da -4 Piccolissima a +4 Colossale): NON è lineare oltre Grande/Piccola, raddoppia agli
    // estremi (Mastodontica -4, Colossale -8, Minuta +4, Piccolissima +8) invece di continuare a scalare
    // di 1 per passo. Diverso dal modificatore di Lotta/Presa, che è lineare (vedi uso diretto di
    // "4*taglia" per LTT) e non va toccato.
    private static final int[] SIZE_MOD_CA_ATTACCO = {8, 4, 2, 1, 0, -1, -2, -4, -8};

    /** Modificatore di taglia per CA/attacco dalla tabella non lineare SIZE_MOD_CA_ATTACCO. */
    private int sizeModCaAttacco(int taglia) {
        int idx = Math.max(0, Math.min(SIZE_MOD_CA_ATTACCO.length - 1, taglia + 4));
        return SIZE_MOD_CA_ATTACCO[idx];
    }

    /**
     * Taglia effettiva del personaggio. Parte dalla taglia base (la
     * personaggio_label TAGLIA, rappresentata da una label con item nullo).
     * Un item con label TAGLIA SOSTITUISCE la base. Infine le label ADD_TAGLIA
     * sommano/sottraggono alla taglia effettiva.
     */
    public Integer getTaglia(List<ItemLabel> taglia) {
        try {
            List<ItemLabel> set = taglia.stream()
                    .filter(c -> c != null && Constants.ITEM_LABEL_TAGLIA.equals(c.getLabel()))
                    .toList();

            // taglia base: il SET del personaggio (item == null)
            int effettiva = set.stream()
                    .filter(x -> x.getItem() == null)
                    .map(ItemLabel::getValore)
                    .map(this::parseTaglia)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(0);

            // override da item CLASSE: quello al livello più alto
            TreeMap<Integer, ItemLabel> tagliaPerLivello = set.stream()
                    .filter(c -> c.getItem() != null && c.getItem().getTipo() == TipoItem.CLASSE)
                    .map(c -> new AbstractMap.SimpleEntry<>(estraiLvlDaTaglia(c), c))
                    .filter(e -> e.getKey() != null)
                    .collect(Collectors.toMap(
                            AbstractMap.SimpleEntry::getKey,
                            AbstractMap.SimpleEntry::getValue,
                            (a, b) -> a,          // in caso di stesso LVL tieni il primo
                            TreeMap::new          // <Integer, ItemLabel> dedotti correttamente
                    ));
            ItemLabel tagliaDiClasse = tagliaPerLivello.isEmpty()
                    ? null
                    : tagliaPerLivello.lastEntry().getValue();

            // override da item NON di classe, NON trasformazione (es. trait razziale, oggetto magico)
            ItemLabel tagliaAltro = set.stream()
                    .filter(x -> x.getItem() != null
                            && x.getItem().getTipo() != TipoItem.CLASSE
                            && x.getItem().getTipo() != TipoItem.FRUTTO
                            && x.getItem().getTipo() != TipoItem.FORMA
                            && x.getItem().getTipo() != TipoItem.TRASFORMAZIONE)
                    .findFirst()
                    .orElse(null);

            // override da FRUTTO / FORMA / TRASFORMAZIONE: priorità massima (sovrascrive tutto)
            ItemLabel tagliaFrutto = set.stream()
                    .filter(x -> x.getItem() != null && (
                            TipoItem.FRUTTO.equals(x.getItem().getTipo())
                                    || TipoItem.FORMA.equals(x.getItem().getTipo())
                                    || TipoItem.TRASFORMAZIONE.equals(x.getItem().getTipo())))
                    .findFirst()
                    .orElse(null);

            // un item con TAGLIA sostituisce la base (ordine crescente di priorità)
            if (tagliaDiClasse != null) {
                effettiva = Integer.parseInt(tagliaDiClasse.getValore());
            }
            if (tagliaAltro != null) {
                effettiva = Integer.parseInt(tagliaAltro.getValore());
            }
            if (tagliaFrutto != null) {
                effettiva = Integer.parseInt(tagliaFrutto.getValore());
            }

            // ADD_TAGLIA: incrementi/decrementi sulla taglia effettiva
            int add = taglia.stream()
                    .filter(c -> c != null && Constants.ITEM_LABEL_ADD_TAGLIA.equals(c.getLabel()))
                    .map(c -> parseTaglia(c.getValore()))
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();

            return effettiva + add;
        } catch (Exception e) {
            return 0;
        }

    }

    /** Parsa una taglia come intero, null se non è un numero valido. */
    private Integer parseTaglia(String s) {
        if (s == null) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Taglia base scritta nelle info personaggio (la personaggio_label TAGLIA, item == null,
     * stesso valore di partenza usato come "effettiva" in {@link #getTaglia(List)}), non l'attuale
     * taglia effettiva (che tiene conto di eventuali override di classe/oggetto/trasformazione):
     * la differenza taglia-attuale vs taglia-base usata per il malus/bonus COS deve confrontarsi
     * con quanto impostato in anagrafica, non con la taglia "di classe". 0 se non impostata.
     */
    public Integer getTagliaBase(List<ItemLabel> taglia) {
        try {
            return taglia.stream()
                    .filter(c -> c != null && Constants.ITEM_LABEL_TAGLIA.equals(c.getLabel()) && c.getItem() == null)
                    .map(ItemLabel::getValore)
                    .map(this::parseTaglia)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(0);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Il BAB si divide al massimo in 6 attacchi (regola di sistema), anche se il valore di
     * partenza (per bonus da oggetti/buff) permetterebbe altrimenti più iterazioni.
     */
    private static final int MAX_ATTACCHI_ITERATIVI = 6;

    /** Bonus di attacco di ciascun attacco iterativo (-5 ogni 5 punti di BAB, vedi MAX_ATTACCHI_ITERATIVI). */
    public static List<Integer> computeIterativeAttacks(int baseBab, int baseMod) {
        List<Integer> attacks = new ArrayList<>();
        int value = baseBab;
        while (value > 0 && attacks.size() < MAX_ATTACCHI_ITERATIVI) {
            attacks.add(baseMod);
            value -= 5;
            baseMod -= 5;
        }
        return attacks;
    }

    /**
     * Normalizza le formule della lista: espande i riferimenti $VAR (senza ID esplicito)
     * in $&lt;itemId&gt;_VAR, lasciando invariati i riferimenti già qualificati come $123_VAR.
     * Esempio: "$1111_QTA+$QTA+@CAR*3" con itemId=456 → "$1111_QTA+$456_QTA+@CAR*3"
     */
    void espandiIdInFormule(List<ModificatoreDTO> mods) {
        for (ModificatoreDTO m : mods) {
            if (m.getFormula() == null || !m.getFormula().contains("$") || m.getItemId() == null) continue;
            m.setFormula(m.getFormula().replaceAll("\\$(?!\\d+_)", "\\$" + m.getItemId() + "_"));
        }
    }

    /**
     * Blocco "Temporaneo" (bonus/malus ad hoc inserito a mano su stat.getValore()): ripetuto
     * identico in calcolaCaratteristica/calcoloTiroSalvezza/calcolaClasseArmatura/
     * calcolaBonusAttacco/calcolaAttributo/calcolaDadiVita.
     */
    private void aggiungiTemporaneo(List<ModificatoreDTO> lista, StatValue stat) {
        if (!stat.getValore().equals("0")) {
            lista.add(new ModificatoreDTO(null, stat.getStat().getId(), Integer.parseInt(stat.getValore()), null, null, TipoModificatore.VALORE, false, Constants.MODIFICATORE_TEMP, null, null));
        }
    }

    /**
     * Converte un valore di caratteristica nel suo modificatore D&D 3.5. Per ora è solo (valore-10)/2.
     */
    private int valoreToMod(int valore) {
        return (valore - 10) / 2;
    }

    /**
     * ModificatoreDTO "base" derivato dalla caratteristica collegata a stat.getMod() (es. DES per
     * CA, COS per PF...), o null se stat non ha una caratteristica di riferimento o questa non è
     * (ancora) in carList. Ripetuto identico in calcolaClasseArmatura/calcolaBonusAttacco/
     * calcolaAttributo/calcolaDadiVita (una delle copie non controllava stat.getMod() == null
     * prima di usarlo, rischiando un NPE: qui è centralizzato e sempre sicuro).
     */
    private ModificatoreDTO baseModDaCaratteristica(StatValue stat, List<CaratteristicaDTO> carList) {
        if (stat.getMod() == null) return null;
        return carList.stream().filter(c -> c.getId().equals(stat.getMod().getId()))
                .findFirst()
                .map(x -> new ModificatoreDTO(null, x.getId(), x.getModificatore(), null, null, null, true, x.getLabel(), null, null))
                .orElse(null);
    }

    /**
     * Estrae da modsDto tutti i modificatori MOLTIPLICA/DIVIDI e li raggruppa in UN UNICO
     * ModificatoreDTO (invece di lasciarne N separati, uno per fonte, fragili da leggere e da
     * estendere con più di una fonte): la nota elenca ogni fonte con il suo ×N/÷N (una riga
     * ciascuna, mostrata in piccolo/grigio a frontend sotto il titolo), la formula è il fattore
     * ×/÷ NETTO combinato — prodotto dei moltiplicatori diviso prodotto dei divisori, ridotto ai
     * minimi termini e applicato UNA SOLA VOLTA (non più una troncatura per ogni divisore in
     * sequenza) — es. ×6 e ÷4 diventano "×1.5", non "×6 ÷4". Il titolo (item) è "Moltiplicatore"
     * se il fattore netto è > 1, altrimenti "Divisore".
     * <p>
     * Il fattore ×/÷ va applicato al VALORE grezzo ({@code valoreBase}, già calcolato da
     * VALORE+MOD), non al modificatore ((valore-10)/2): quest'ultimo resta una conversione
     * derivata automaticamente più avanti (vedi {@link #valoreToMod}), non va ricalcolato qui. Il
     * valore del modificatore restituito è quindi il delta da SOMMARE a {@code valoreBase} per
     * ottenere il valore finale.
     * <p>
     * Ritorna null se non c'è nessun MOLTIPLICA/DIVIDI (con nota, valore o formula validi).
     */
    private ModificatoreDTO estraiModificatoreMoltiplicaDividi(String statId, List<ModificatoreDTO> modsDto, VariabiliDTO variabili, int valoreBase) {
        List<ModificatoreDTO> candidati = new ArrayList<>(modsDto.stream()
                .filter(m -> TipoModificatore.MOLTIPLICA.equals(m.getTipo()) || TipoModificatore.DIVIDI.equals(m.getTipo()))
                .toList());
        if (candidati.isEmpty()) return null;
        risolviFormule(candidati, variabili.mappa());

        List<ModificatoreDTO> moltiplicatoriDto = candidati.stream()
                .filter(x -> x.getNota() == null && TipoModificatore.MOLTIPLICA.equals(x.getTipo()) && x.getValore() != null)
                .toList();
        List<ModificatoreDTO> divisoriDto = candidati.stream()
                .filter(x -> x.getNota() == null && TipoModificatore.DIVIDI.equals(x.getTipo()) && x.getValore() != null && x.getValore() != 0)
                .toList();
        if (moltiplicatoriDto.isEmpty() && divisoriDto.isEmpty()) return null;

        int numeratore = moltiplicatoriDto.stream().mapToInt(ModificatoreDTO::getValore).reduce(1, (a, b) -> a * b);
        int denominatore = divisoriDto.stream().mapToInt(ModificatoreDTO::getValore).reduce(1, (a, b) -> a * b);
        int mcd = mcd(numeratore, denominatore);
        numeratore /= mcd;
        denominatore /= mcd;

        int nuovoValore = valoreBase * numeratore / denominatore;

        String fonti = Stream.concat(
                moltiplicatoriDto.stream().map(m -> m.getItem() + " (×" + m.getValore() + ")"),
                divisoriDto.stream().map(d -> d.getItem() + " (÷" + d.getValore() + ")")
        ).collect(Collectors.joining("\n"));

        boolean nettoMoltiplicatore = numeratore > denominatore;
        String fattoreFinale = (nettoMoltiplicatore ? "×" : "÷")
                + formattaRapporto(nettoMoltiplicatore ? numeratore : denominatore, nettoMoltiplicatore ? denominatore : numeratore);

        return new ModificatoreDTO(null, statId, nuovoValore - valoreBase, fattoreFinale,
                fonti, TipoModificatore.MOLTIPLICA, false, nettoMoltiplicatore ? "Moltiplicatore" : "Divisore", null, null);
    }

    /** Massimo comun divisore (Euclide), per ridurre ai minimi termini il fattore ×/÷ combinato. */
    private static int mcd(int a, int b) {
        return b == 0 ? Math.max(a, 1) : mcd(b, a % b);
    }

    /**
     * Formatta un rapporto a/b come decimale ESATTO (non troncato: il calcolo di runningMod non
     * passa comunque da questa stringa, usa numeratore/denominatore interi), senza ".0" superfluo
     * se è un intero (4/2 → "2"). Il troncamento a un decimale è solo estetico e va fatto lato
     * frontend alla visualizzazione, non qui.
     */
    private static String formattaRapporto(int a, int b) {
        double r = (double) a / b;
        return r == Math.floor(r) ? String.valueOf((long) r) : String.valueOf(r);
    }

    /**
     * Costruisce "variabili": $id->valore (contatori item) + @id->valore (contatori item,
     * ripetuti anche come @ per le formule di CA che li referenziano così) + @id->modificatore
     * (caratteristiche), usata per risolvere le formule dei modificatori in TUTTI i metodi
     * calcola*. Costruita UNA SOLA VOLTA per personaggio da PersonaggioService (non più
     * ricostruita identica dentro ogni singolo calcola*) e passata già pronta: chi ha bisogno di
     * aggiungere altro (es. la taglia: calcolata fuori da PersonaggioService una volta sola, con
     * getTaglia/getTagliaBase, e messa qui con setTagliaAttuale/setTagliaBase PRIMA di qualunque
     * chiamata a calcolaCaratteristica/calcolaClasseArmatura/calcolaBonusAttacco) scrive
     * direttamente in questa stessa istanza, condivisa, invece di calcolare/duplicare altrove.
     */
    public VariabiliDTO costruisciVariabili(List<ContatoreItemDTO> contatoriItem, List<CaratteristicaDTO> carList) {
        VariabiliDTO variabili = new VariabiliDTO();
        variabili.setAll(contatoriItem.stream()
                .collect(Collectors.toMap(x -> "$".concat(x.getId()), x -> x.getValore().toString(), (a, b) -> a)));
        variabili.setAll(contatoriItem.stream()
                .collect(Collectors.toMap(x -> "@".concat(x.getId()), x -> x.getValore().toString(), (a, b) -> a)));
        variabili.setAll(carList.stream()
                .collect(Collectors.toMap(x -> "@".concat(x.getId()), x -> x.getModificatore().toString(), (a, b) -> a)));
        return variabili;
    }

    /**
     * Risolve le formule con variabili ($... o @...) di ciascun modificatore nel contesto dato
     * (via calcoloService.calcola), sostituendo formula/valore; se la risoluzione fallisce, il
     * modificatore viene scartato dalla lista. Usa un Iterator per rimuovere in sicurezza durante
     * l'iterazione: le 4 copie di questo ciclo che c'erano prima (calcolaCaratteristica,
     * calcoloTiroSalvezza, calcolaAbilita, calcolaClasseArmatura) rimuovevano dalla lista dentro
     * un for-each/forEach sulla lista stessa, il che lancia ConcurrentModificationException non
     * appena capita davvero una formula da scartare.
     */
    private void risolviFormule(List<ModificatoreDTO> lista, Map<String, String> contesto) {
        Iterator<ModificatoreDTO> it = lista.iterator();
        while (it.hasNext()) {
            ModificatoreDTO m = it.next();
            if (m.getFormula() != null && (m.getFormula().contains("$") || m.getFormula().indexOf('@') >= 0)) {
                try {
                    m.setFormula(calcoloService.calcola(m.itemIdInFormula(), contesto));
                    m.setValore(Integer.parseInt(m.getFormula()));
                } catch (Exception e) {
                    it.remove();
                }
            }
        }
    }

    /** Livello dell'item LIVELLO padre della classe che porta questa label TAGLIA, per ordinare
     *  gli override di taglia per livello raggiunto (getTaglia tiene quello più alto). */
    private static Integer estraiLvlDaTaglia(ItemLabel c) {
        if (c == null || c.getItem() == null) return null;

        Item livello = c.getItem()
                .getParent()                                // Collection<Collegamento>
                .stream()
                .map(Collegamento::getItemSource)           // Item
                .filter(src -> src != null && src.getTipo() == TipoItem.LIVELLO)
                .findFirst()
                .orElse(null);

        if (livello == null) return null;

        String lvlStr = livello.getLabel(Constants.ITEM_LIVELLO_LVL); // es. "5"
        if (lvlStr == null) return null;

        try {
            return Integer.valueOf(lvlStr.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Malus PF per un livello "maledetto": il minimo tra i PF assegnati a quel livello e la
     *  media dei dadi vita persi (vedi calcolaMalus). */
    private static ModificatoreDTO getMalusPF(Item itemLivello) {
        String dadoVita = itemLivello.getLabel(Constants.ITEM_LABEL_DADI_VITA);
        Modificatore modificatorePF = itemLivello.getModificatore(Constants.ITEM_LABEL_PUNTI_FERITA);
        String pfLivelloString = modificatorePF != null ? modificatorePF.getValore() : "0";
        int pfLivello = Integer.parseInt(pfLivelloString.trim());

        int malus = Math.min(pfLivello, calcolaMalus(dadoVita));

        return new ModificatoreDTO(null, "PF", -1 * malus, null, null, TipoModificatore.VALORE, true, itemLivello.getNome() + " Maledetto", null, itemLivello.getTipo());
    }

    /** Media SRD di un dado vita, arrotondata per eccesso (non aritmetica pura: d6 → 4, non 3.5). */
    private static int mediaDadoDND35(int M) {
        return switch (M) {
            case 4 -> 3;
            case 6 -> 4;
            case 8 -> 5;
            case 10 -> 6;
            case 12 -> 7;
            default -> (M + 1) / 2;
        };
    }

    /** Punti vita persi per un dado vita "NdM" (es. "2d6"), usando la media SRD di ciascun dado. */
    private static int calcolaMalus(String dadoVita) {
        String[] parts = dadoVita.split("d");
        int N = Integer.parseInt(parts[0]);
        int M = Integer.parseInt(parts[1]);

        int media = mediaDadoDND35(M);
        return N * media;
    }

}
