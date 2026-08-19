package it.fin8.gdrsheet.service;

import it.fin8.gdrsheet.config.Constants;
import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.dto.CalcoloResponse;
import it.fin8.gdrsheet.dto.CaratteristicaDTO;
import it.fin8.gdrsheet.dto.DatiPersonaggioDTO;
import it.fin8.gdrsheet.dto.InfoClasseDTO;
import it.fin8.gdrsheet.dto.InfoLivelliDTO;
import it.fin8.gdrsheet.entity.Item;
import it.fin8.gdrsheet.mapper.StatMapper;
import it.fin8.gdrsheet.repository.ItemLabelRepository;
import it.fin8.gdrsheet.repository.ItemRepository;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class CalcoloService {

    @Autowired
    private StatMapper statMapper;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UtilService utilService;

    @Autowired
    private ItemLabelRepository itemLabelRepository;


    private static final Pattern PATTERN_PLACEH = Pattern.compile("@\\w+");
    private static final Pattern PATTERN_DICE = Pattern.compile("\\d+d\\d+(?:\\d+)?");
    private static final Pattern PATTERN_NUMBER = Pattern.compile("^[+-]?\\d+$");
    // Termine "isolato" di dado (es. "+2d8", "-1d6"), usato da consolidaDadiRipetuti per
    // riconoscere e accorpare termini con la stessa faccia dopo la sostituzione delle variabili.
    private static final Pattern PATTERN_TERM_DADO = Pattern.compile("^([+-])?(\\d+)d(\\d+)$");
    // Fattore di dado SENZA segno (il segno del termine si applica una sola volta al risultato
    // del prodotto, non a ogni fattore) — usato da valutaTermine per riconoscere "10d8" dentro
    // un fattore di moltiplicazione come "2*10d8".
    private static final Pattern PATTERN_DADO_NUDO = Pattern.compile("^(\\d+)d(\\d+)$");
    // Termine "isolato" puramente numerico (es. "+235", "-4.5"), usato da consolidaDadiRipetuti
    // per sommare in un solo numero tutti i termini non-dado di una formula già risolta.
    private static final Pattern PATTERN_TERM_NUMERO = Pattern.compile("^([+-])?(\\d+(?:\\.\\d+)?)$");

    // Calcolo a scaglioni progressivi (tipo scaglioni IRPEF): "x=<espr>;[{'<=N','formula con x'}, ...]".
    // Ogni scaglione applica la SUA formula solo alla porzione di x compresa tra la soglia
    // precedente e la propria; oltre l'ultimo scaglione definito il contributo è 0.
    private static final Pattern PATTERN_SCAGLIONI =
            Pattern.compile("^x\\s*=\\s*([^;]+);\\s*\\[(.*)]\\s*$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern PATTERN_SCAGLIONE =
            Pattern.compile("\\{\\s*'<=\\s*([-+]?[0-9]*\\.?[0-9]+)'\\s*,\\s*'([^']*)'\\s*}");

    // Funzioni utilizzabili nelle formule: arrotondamento (ECCESSO/DIFETTO/TRONCATO, 1 argomento)
    // e min/max tra due valori (MIN/MAX, 2 argomenti) — es. "MIN(@LVL,20)".
    private static final Function FN_ECCESSO = new Function("ECCESSO", 1) {
        @Override public double apply(double... args) { return Math.ceil(args[0]); }
    };
    private static final Function FN_DIFETTO = new Function("DIFETTO", 1) {
        @Override public double apply(double... args) { return Math.floor(args[0]); }
    };
    private static final Function FN_TRONCATO = new Function("TRONCATO", 1) {
        @Override public double apply(double... args) { return (double) (long) args[0]; }
    };
    private static final Function FN_MIN = new Function("MIN", 2) {
        @Override public double apply(double... args) { return Math.min(args[0], args[1]); }
    };
    private static final Function FN_MAX = new Function("MAX", 2) {
        @Override public double apply(double... args) { return Math.max(args[0], args[1]); }
    };
    private static final Function[] FUNZIONI = {FN_ECCESSO, FN_DIFETTO, FN_TRONCATO, FN_MIN, FN_MAX};

    /**
     * Metodo base: sostituisce nella formula le chiavi della mappa (es. "@CAR", "$1983_QTA")
     * con i rispettivi valori, poi valuta l'espressione numerica risultante.
     * Le chiavi più lunghe vengono sostituite prima per evitare match parziali
     * (es. "@PESO_TOTALE" prima di "@PESO").
     */
    // Prefisso "cd"/"CD" scritto per errore dentro al campo formula (es. "cd 10+@LVL"): capita
    // spesso perché il campo si chiama già "Formula CD", non è una variabile riconosciuta e
    // rompe la valutazione facendola tornare a 0 in silenzio. "CD" non è più una stat valida
    // (la vecchia "CD Incantesimi" è stata rimossa), quindi si può togliere in sicurezza.
    private static final Pattern PATTERN_PREFISSO_CD = Pattern.compile("^\\s*cd\\s+", Pattern.CASE_INSENSITIVE);

    public String calcola(String formula, Map<String, String> valori) {
        if (formula == null || formula.isBlank()) return "0";

        // 1. Sostituisci le variabili (chiavi più lunghe prima)
        String replaced = PATTERN_PREFISSO_CD.matcher(formula).replaceFirst("");
        List<Map.Entry<String, String>> entries = new ArrayList<>(valori.entrySet());
        entries.sort((a, b) -> b.getKey().length() - a.getKey().length());
        for (Map.Entry<String, String> e : entries) {
            if (e.getValue() != null) {
                replaced = replaced.replace(e.getKey(), e.getValue());
            }
        }

        // Calcolo a scaglioni progressivi: intercetta la sintassi PRIMA di exp4j,
        // che non può valutare array/oggetti letterali come "[{'<=50','x*50/1000'},...]".
        Matcher scaglioniMatcher = PATTERN_SCAGLIONI.matcher(replaced.trim());
        if (scaglioniMatcher.matches()) {
            return String.valueOf(calcolaScaglioni(scaglioniMatcher));
        }

        // 2. Estrai e salva la parte col dado (es: 1d8)
        Matcher diceMatcher = PATTERN_DICE.matcher(replaced);
        String dicePart = "";
        if (diceMatcher.find()) {
            dicePart = diceMatcher.group();
            replaced = replaced.replace(dicePart, "");
        }

        // 3. Pulizia formula numerica (rimuove spazi)
        String numericExpr = replaced.replaceAll("\\s+", "");

        long result = 0;
        if (!numericExpr.isBlank()) {
            try {
                double eval = new ExpressionBuilder(numericExpr)
                        .functions(FUNZIONI)
                        .build().evaluate();
                result = (long) Math.floor(eval);
            } catch (Exception e) {
            }
        }

        // 4. Ritorna la formula finale
        if (!dicePart.isBlank()) {
            return result != 0 ? dicePart + (result > 0 ? "+" : "") + result : dicePart;
        } else {
            return String.valueOf(result);
        }
    }

    /**
     * Valuta la sintassi a scaglioni progressivi "x=&lt;espr&gt;;[{'&lt;=N','formula con x'}, ...]".
     * Ogni scaglione riceve come "x" solo la porzione del valore compresa tra la soglia
     * precedente e la propria (non il valore intero); la porzione oltre l'ultimo scaglione
     * definito non contribuisce (equivale a moltiplicarla per 0).
     */
    private long calcolaScaglioni(Matcher scaglioniMatcher) {
        double x;
        try {
            String xExpr = scaglioniMatcher.group(1).trim().replaceAll("\\s+", "");
            x = new ExpressionBuilder(xExpr).functions(FUNZIONI).build().evaluate();
        } catch (Exception e) {
            return 0;
        }

        record Scaglione(double soglia, String formula) {}
        List<Scaglione> scaglioni = new ArrayList<>();
        Matcher sm = PATTERN_SCAGLIONE.matcher(scaglioniMatcher.group(2));
        while (sm.find()) {
            try {
                scaglioni.add(new Scaglione(Double.parseDouble(sm.group(1)), sm.group(2)));
            } catch (NumberFormatException ignored) {
            }
        }
        scaglioni.sort(Comparator.comparingDouble(Scaglione::soglia));

        double totale = 0;
        double sogliaPrecedente = 0;
        for (Scaglione s : scaglioni) {
            double porzione = Math.max(0, Math.min(x, s.soglia()) - sogliaPrecedente);
            if (porzione > 0 && !s.formula().isBlank()) {
                String exprScaglione = s.formula()
                        .replaceAll("\\bx\\b", String.valueOf(porzione))
                        .replaceAll("\\s+", "");
                try {
                    totale += new ExpressionBuilder(exprScaglione).functions(FUNZIONI).build().evaluate();
                } catch (Exception ignored) {
                }
            }
            sogliaPrecedente = s.soglia();
        }
        // porzione oltre l'ultimo scaglione definito: nessun contributo (implicitamente "x*0")

        return (long) Math.floor(totale);
    }

    /**
     * Wrapper: converte la lista di CaratteristicaDTO in mappa chiave→valore
     * (chiave = "@" + id, valore = modificatore come stringa) e chiama il metodo base.
     */
    public String calcola(String formula, List<CaratteristicaDTO> caratteristiche) {
        if (formula == null || formula.isBlank()) return "0";
        Map<String, String> valori = new LinkedHashMap<>();
        for (CaratteristicaDTO c : caratteristiche) {
            if (c.getId() != null) {
                valori.put("@" + c.getId(), String.valueOf(c.getModificatore() != null ? c.getModificatore() : 0));
            }
        }
        return calcola(formula, valori);
    }

    /**
     * Overload per DTO personaggio
     */
    public String calcola(String formula, DatiPersonaggioDTO dati) {
        return calcola(formula, costruisciMappaVariabili(dati));
    }

    /**
     * Costruisce la mappa chiave→valore (chiave "@" + id) usata sia da calcola() per il risultato
     * numerico sia da sostituisciVariabili() per il testo "leggibile" della formula — unica fonte
     * di verità, invece di ricalcolarla in due posti (o, come prima, farla ricostruire due volte
     * in modo diverso: una qui e una — mai del tutto uguale, vedi sostituisciVariabili — lato
     * frontend in risolviFormulaDanno/Utils.ts).
     */
    private Map<String, String> costruisciMappaVariabili(DatiPersonaggioDTO dati) {
        Map<String, String> valori = costruisciMappaCaratteristiche(dati);
        // dati.getVariabili(): TUTTO il resto risolvibile per questo personaggio (contatori item,
        // taglia, differenza taglia, livello per classe @LIVELLO_NM_/MNM_/TOT_/MAX_/CASTER_<id>...
        // vedi ModificatoriService.costruisciVariabili/PersonaggioService) — chiavi GIÀ prefissate
        // con "@", niente da ritrasformare. Prima di questo fix non veniva mai letta: le formule
        // (es. sui contatori item, o sulla taglia) restituivano sempre 0 in silenzio (eccezione
        // exp4j catturata sopra) perché la variabile non veniva mai sostituita nella stringa.
        // putIfAbsent: le entry già risolte sopra (caratteristiche/LVL/DV/PESO...) restano quelle.
        if (dati.getVariabili() != null) {
            dati.getVariabili().forEach((k, v) -> {
                if (v != null) valori.putIfAbsent(k, String.valueOf(v));
            });
        }
        return valori;
    }

    /**
     * Solo la parte "leggibile come nome" della mappa variabili (caratteristiche/bonusAttacco/
     * attributi/PESO/ETA/ALTEZZA/LVL/DV) — SENZA il merge di dati.getVariabili(). Usata sia da
     * costruisciMappaVariabili() sia da formulaLeggibile(), che deve sapere QUALI chiavi non
     * toccare (dati.getVariabili() include ANCHE le caratteristiche — vedi
     * ModificatoriService.costruisciVariabili/PersonaggioService — quindi non basta "esserci
     * dentro" per capire se una chiave è "opaca" o no: bisogna escludere esplicitamente queste).
     */
    private Map<String, String> costruisciMappaCaratteristiche(DatiPersonaggioDTO dati) {
        List<CaratteristicaDTO> caratteristiche = new ArrayList<>(dati.getCaratteristiche());
        if (dati.getBonusAttacco() != null) {
            caratteristiche.addAll(dati.getBonusAttacco().stream()
                    .map(statMapper::toCaratteristicaDTO)
                    .toList());
        }
        if (dati.getAttributi() != null) {
            caratteristiche.addAll(dati.getAttributi().stream()
                    .map(statMapper::toCaratteristicaDTO)
                    .toList());
        }

        // Variabili anagrafiche/peso disponibili nelle formule: PESO, ETA, ALTEZZA, PESO_TOTALE
        variabiliPersonaggio(dati.getInfo(), dati.getPesoTotale())
                .forEach((k, v) -> caratteristiche.add(new CaratteristicaDTO(k, k, null, v, null, null)));

        List<Item> initialRoots = itemRepository.findAllByPersonaggioIdWithChild(dati.getId());
        caratteristiche.add(new CaratteristicaDTO("LVL", "Livello", null, Integer.parseInt(String.valueOf(getLivelli(initialRoots, dati.getId()).getLivello())), null, null));
        // DV (Dadi Vita totali): esclusa di proposito dalla lista "attributi" generica (gestita a
        // parte in dadiVita), va aggiunta esplicitamente qui per essere usabile nelle formule.
        if (dati.getDadiVita() != null && dati.getDadiVita().getTotale() != null) {
            caratteristiche.add(new CaratteristicaDTO("DV", "Dadi Vita", null, dati.getDadiVita().getTotale(), null, null));
        }

        // Mappa base: stessa trasformazione dell'overload per lista (chiave "@" + id).
        Map<String, String> valori = new LinkedHashMap<>();
        for (CaratteristicaDTO c : caratteristiche) {
            if (c.getId() != null) {
                valori.put("@" + c.getId(), String.valueOf(c.getModificatore() != null ? c.getModificatore() : 0));
            }
        }
        return valori;
    }

    /**
     * Sostituisce le variabili nella formula con i valori del personaggio SENZA valutare
     * l'espressione aritmetica: serve a mostrare all'utente la formula "leggibile" (es.
     * "2d8+10d8+235+460" invece di "2d8+@1191_CARICHEd8+@FOR+@SAG"), a differenza di calcola()
     * che restituisce il solo risultato numerico finale. Usa la STESSA mappa di variabili di
     * calcola() (stessa fonte di verità) — prima questa sostituzione era duplicata (e, sulle
     * variabili con cifre/underscore come i contatori item, sbagliata: collisioni di sottostringa
     * tipo "@CAR" dentro "@1191_CARICHE") lato frontend in risolviFormulaDanno (Utils.ts).
     * Chiavi più lunghe sostituite prima, come calcola(), per lo stesso motivo (evitare che una
     * chiave più corta "mangi" un pezzo di una più lunga che la contiene per intero).
     */
    public String sostituisciVariabili(String formula, DatiPersonaggioDTO dati) {
        return sostituisciVariabili(formula, costruisciMappaVariabili(dati));
    }

    private String sostituisciVariabili(String formula, Map<String, String> valori) {
        if (formula == null || formula.isBlank()) return "";
        List<Map.Entry<String, String>> entries = new ArrayList<>(valori.entrySet());
        entries.sort((a, b) -> b.getKey().length() - a.getKey().length());
        String out = formula;
        for (Map.Entry<String, String> e : entries) {
            if (e.getValue() != null) out = out.replace(e.getKey(), e.getValue());
        }
        out = consolidaDadiRipetuti(out);
        return out.replace("@", "").replace("*", "x").replace("0.5", "½");
    }

    /**
     * Formula "leggibile" per il sotto-testo (es. "2d8+10d8+FOR+SAG" sotto il valore calcolato
     * "22d8+1390"): sostituisce SOLO le variabili "opache" — contatori item, taglia, livelli per
     * classe (dati.getVariabili(), mai leggibili come nome, es. "@1991_CARICHE") — con il loro
     * valore numerico, lasciando invece SIMBOLICHE le caratteristiche/attributi (@FOR, @SAG...)
     * che sono già leggibili di per sé. Nessuna valutazione aritmetica: dadi e moltiplicatori
     * restano testo esattamente come scritti (solo cosmetica, come testoFormula in Utils.ts).
     */
    public String formulaLeggibile(String formula, DatiPersonaggioDTO dati) {
        return formulaLeggibile(formula, dati, costruisciMappaCaratteristiche(dati).keySet());
    }

    private String formulaLeggibile(String formula, DatiPersonaggioDTO dati, Set<String> chiaviLeggibili) {
        if (formula == null || formula.isBlank()) return "";
        // chiaviLeggibili: da NON sostituire. dati.getVariabili() include ANCHE le caratteristiche
        // (vedi costruisciMappaCaratteristiche), quindi vanno escluse esplicitamente qui, non
        // basta guardare se una chiave "esiste" in dati.getVariabili().
        String out = formula;
        if (dati.getVariabili() != null) {
            List<Map.Entry<String, Integer>> entries = new ArrayList<>(dati.getVariabili().entrySet());
            entries.sort((a, b) -> b.getKey().length() - a.getKey().length());
            for (Map.Entry<String, Integer> e : entries) {
                if (e.getValue() == null || chiaviLeggibili.contains(e.getKey())) continue;
                out = out.replace(e.getKey(), String.valueOf(e.getValue()));
            }
        }
        return out.replace("@", "").replace("$", "")
                .replace("*", "x").replace("0.5", "½")
                .replace("MSC", "Mischia").replace("GTT", "Distanza").replace("INF", "∞");
    }

    /**
     * Un solo giro di costruzione delle mappe (niente re-query del DB per ogni pezzo, come
     * accadrebbe chiamando calcola()/sostituisciVariabili()/formulaLeggibile() separatamente, che
     * ognuno rifarebbe costruisciMappaCaratteristiche() — e quindi la query sugli item del
     * personaggio per il livello — da capo). Unico punto usato da CalcoloController.
     */
    public CalcoloResponse calcolaCompleto(String formula, DatiPersonaggioDTO dati) {
        Map<String, String> mappaCaratteristiche = costruisciMappaCaratteristiche(dati);
        Map<String, String> mappaCompleta = new LinkedHashMap<>(mappaCaratteristiche);
        if (dati.getVariabili() != null) {
            dati.getVariabili().forEach((k, v) -> {
                if (v != null) mappaCompleta.putIfAbsent(k, String.valueOf(v));
            });
        }

        CalcoloResponse resp = new CalcoloResponse();
        resp.setFormula(formula);
        resp.setRisultato(calcola(formula, mappaCompleta));
        resp.setFormulaRisolta(sostituisciVariabili(formula, mappaCompleta));
        resp.setFormulaLeggibile(formulaLeggibile(formula, dati, mappaCaratteristiche.keySet()));
        return resp;
    }

    /**
     * Valuta le moltiplicazioni della formula (dopo la sostituzione delle variabili), accorpa i
     * termini con la stessa faccia di dado e somma i termini puramente numerici in uno solo — es.
     * "2d8+2*10d8+2*235+2*460" (da "2d8+2*@1991_CARICHEd8+2*@FOR+2*@SAG") diventa "22d8+1390":
     * "2*10d8" scala il NUMERO di dadi ("20d8"), "2d8+20d8" si accorpa in "22d8", e "2*235"/"2*460"
     * (diventati "470"/"920") si sommano in "1390". Se non c'è nulla da accorpare/sommare e
     * nessun termine è stato modificato, resta invariato: mai "inventare" un risultato su
     * qualcosa che non si sa risolvere.
     */
    private String consolidaDadiRipetuti(String testo) {
        if (testo == null || testo.isBlank()) return testo;
        String norm = testo.trim();
        if (norm.charAt(0) != '+' && norm.charAt(0) != '-') norm = "+" + norm;
        String[] termini = norm.split("(?=[+-])");

        Map<Integer, Integer> sommaPerFaccia = new LinkedHashMap<>();
        Map<Integer, Integer> occorrenzePerFaccia = new HashMap<>();
        List<String> altriTermini = new ArrayList<>();
        boolean qualcosaCambiato = false;
        for (String t : termini) {
            if (t.isBlank()) continue;
            String valutato = valutaTermine(t.trim());
            if (!valutato.equals(t.trim())) qualcosaCambiato = true;
            Matcher m = PATTERN_TERM_DADO.matcher(valutato);
            if (m.matches()) {
                int segno = "-".equals(m.group(1)) ? -1 : 1;
                int faccia = Integer.parseInt(m.group(3));
                sommaPerFaccia.merge(faccia, segno * Integer.parseInt(m.group(2)), Integer::sum);
                occorrenzePerFaccia.merge(faccia, 1, Integer::sum);
            } else {
                altriTermini.add(valutato);
            }
        }

        // Somma dei termini puramente numerici (es. "+235"+"+460" -> "+695"): a differenza dei
        // dadi, un numero vale uguale in qualunque ordine/posizione, niente da capire lì.
        double sommaNumeri = 0;
        int numeriContati = 0;
        List<String> altriNonNumerici = new ArrayList<>();
        for (String t : altriTermini) {
            Matcher nm = PATTERN_TERM_NUMERO.matcher(t);
            if (nm.matches()) {
                double v = Double.parseDouble(nm.group(2));
                sommaNumeri += "-".equals(nm.group(1)) ? -v : v;
                numeriContati++;
            } else {
                altriNonNumerici.add(t);
            }
        }

        boolean qualcosaDaAccorpare = occorrenzePerFaccia.values().stream().anyMatch(n -> n > 1);
        boolean numeriDaAccorpare = numeriContati > 1;
        if (!qualcosaDaAccorpare && !qualcosaCambiato && !numeriDaAccorpare) return testo;

        StringBuilder out = new StringBuilder();
        for (Map.Entry<Integer, Integer> e : sommaPerFaccia.entrySet()) {
            if (e.getValue() == 0) continue;
            out.append(e.getValue() > 0 ? "+" : "").append(e.getValue()).append("d").append(e.getKey());
        }
        if (numeriContati > 0) {
            long intero = Math.round(sommaNumeri);
            String testoNumero = (Math.abs(sommaNumeri - intero) < 1e-9) ? String.valueOf(intero) : String.valueOf(sommaNumeri);
            out.append(sommaNumeri >= 0 ? "+" : "").append(testoNumero);
        }
        for (String t : altriNonNumerici) out.append(t);

        String risultato = out.toString();
        if (risultato.startsWith("+")) risultato = risultato.substring(1);
        return risultato.isEmpty() ? "0" : risultato;
    }

    /**
     * Valuta un singolo termine additivo con segno (es. "+2*10d8", "+2*235", "+FOR" — quest'ultimo
     * senza "*" torna invariato). Se un fattore è un dado ("10d8"), il significato dipende dalla
     * SUA POSIZIONE tra i fattori:
     *  - il dado è l'ULTIMO a comparire (es. "2*10d8", moltiplicatore PRIMA del dado): il
     *    moltiplicatore indica QUANTI dadi tirare, scala il conteggio ("20d8").
     *  - il dado è il PRIMO fattore (es. "1d6*10", moltiplicatore DOPO il dado): significa "tira
     *    una volta e moltiplica il risultato" (es. un moltiplicatore da critico) — semanticamente
     *    diverso da "tira più dadi" (1d6*10 ≠ 10d6, distribuzioni di probabilità diverse), quindi
     *    resta testuale (si vede solo il "*" convertito in "x" più avanti).
     * Se il prodotto è puramente numerico (nessun fattore-dado) si valuta comunque, l'ordine non
     * conta. Più di un fattore-dado, o un fattore non numerico/non risolto (variabile rimasta
     * intatta): il termine torna invariato, per non produrre un risultato inventato.
     */
    private String valutaTermine(String termine) {
        if (!termine.contains("*")) return termine;
        String segno = "";
        String corpo = termine;
        if (corpo.startsWith("+") || corpo.startsWith("-")) {
            segno = corpo.substring(0, 1);
            corpo = corpo.substring(1);
        }

        double coeff = 1;
        Integer diceCount = null;
        Integer diceFace = null;
        int indiceDado = -1;
        String[] fattori = corpo.split("\\*");
        for (int i = 0; i < fattori.length; i++) {
            String fattore = fattori[i].trim();
            Matcher dm = PATTERN_DADO_NUDO.matcher(fattore);
            if (dm.matches()) {
                if (diceCount != null) return termine; // più di un fattore-dado: non gestito
                diceCount = Integer.parseInt(dm.group(1));
                diceFace = Integer.parseInt(dm.group(2));
                indiceDado = i;
                continue;
            }
            try {
                coeff *= Double.parseDouble(fattore);
            } catch (NumberFormatException e) {
                return termine; // fattore non numerico (variabile non risolta): non tocco nulla
            }
        }
        if (diceCount != null && indiceDado == 0) return termine; // dado prima del "*": non tocco

        if (diceCount != null) {
            long nuovoCount = Math.round(diceCount * coeff);
            return segno + nuovoCount + "d" + diceFace;
        }
        double valore = "-".equals(segno) ? -coeff : coeff;
        long intero = Math.round(valore);
        String testoValore = (Math.abs(valore - intero) < 1e-9) ? String.valueOf(intero) : String.valueOf(valore);
        return (valore >= 0 ? "+" : "") + testoValore;
    }

    /**
     * Variabili "anagrafiche" del personaggio usabili nelle formule (@PESO,
     * @ETA, @ALTEZZA, @PESO_TOTALE). Valori arrotondati all'intero. Le voci
     * mancanti/non numeriche vengono omesse.
     */
    public static Map<String, Integer> variabiliPersonaggio(Map<String, String> info, Double pesoTotale) {
        Map<String, Integer> v = new LinkedHashMap<>();
        if (pesoTotale != null) v.put("PESO_TOTALE", (int) Math.round(pesoTotale));
        if (info != null) {
            putIfNum(v, "PESO", info.get("PESO"));
            putIfNum(v, "ALTEZZA", info.get("ALTEZZA"));
            putIfNum(v, "ETA", info.get("ETA"));
        }
        return v;
    }

    private static void putIfNum(Map<String, Integer> map, String key, String raw) {
        if (raw == null || raw.isBlank()) return;
        try {
            map.put(key, (int) Math.round(Double.parseDouble(raw.trim().replace(',', '.'))));
        } catch (NumberFormatException ignored) {
        }
    }

    public InfoLivelliDTO getLivelli(List<Item> initialRoots, Integer idPersonaggio) {
        InfoLivelliDTO out = new InfoLivelliDTO();
        if (initialRoots == null || initialRoots.isEmpty()) {
            out.setLivello(0);
            out.setClassi(Collections.emptyList());
            return out;
        }

        // Chiamato PRIMA del flatten (che è quello che "stampa" DISABLED risolto sull'entity):
        // qui gli item sono ancora "freschi", va controllato esplicitamente per questo personaggio.
        Set<Integer> disabledItemIds = itemLabelRepository.findItemIdsByLabelValoreTrueAndPersonaggio_Id(
                Constants.ITEM_LABEL_DISABILITATO, idPersonaggio);

        // Filtri inline (senza Predicate)
        List<Item> livelliTotali = initialRoots.stream()
                .filter(Objects::nonNull)
                .filter(i -> TipoItem.LIVELLO.equals(i.getTipo()))
                .filter(x -> !disabledItemIds.contains(x.getId()))
                .toList();

        List<Item> livelliAttivi = initialRoots.stream()
                .filter(Objects::nonNull)
                .filter(i -> TipoItem.LIVELLO.equals(i.getTipo()))
                .filter(x -> !disabledItemIds.contains(x.getId()))
                .filter(x -> x.getLabel(Constants.ITEM_LABEL_MALEDIZIONE) == null)
                .toList();

        out.setLivello(Math.max(0, livelliAttivi.size() - 1));

        if (livelliTotali.isEmpty()) {
            out.setClassi(Collections.emptyList());
            return out;
        }

        // Map<Item, Set<Item>> diretta (1 stream)
        Map<Item, Set<Item>> perClasse = livelliTotali.stream()
                .filter(livello -> livello.getLabel(Constants.ITEM_LABEL_CLASSE) != null)
                .collect(Collectors.groupingBy(
                        livello -> {
                            try {
                                return itemRepository.findItemById(
                                        Integer.parseInt(livello.getLabel(Constants.ITEM_LABEL_CLASSE)));
                            } catch (NumberFormatException e) {
                                return null;
                            }
                        },
                        LinkedHashMap::new,
                        Collectors.toSet()  // HashSet sicuro
                ))
                .entrySet().stream()
                .filter(e -> e.getKey() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        // DTOs
        List<InfoClasseDTO> classi = perClasse.entrySet().stream()
                .map(e -> {
                    InfoClasseDTO dto = new InfoClasseDTO();

                    // Tutti i livelli → Set<Integer> (ignora livelli senza LVL_CLASSE valido)
                    Set<Integer> livelliSet = e.getValue().stream()
                            .map(x -> parseIntOrNull(x.getLabel(Constants.ITEM_LIVELLO_LVL_CLASSE)))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());

                    Integer livelloMax = livelliSet.stream()
                            .max(Integer::compareTo).orElse(0);

                    Integer livelloTotale = e.getValue().size();

                    // Livelli NON maledetti per questa classe
                    Integer livelloNonMaledetto = (int) livelliAttivi.stream()
                            .filter(x -> e.getKey().getId().toString()
                                    .equals(x.getLabel(Constants.ITEM_LABEL_CLASSE)))
                            .count();

                    // NUOVO: Max tra i NON maledetti
                    Integer livelloMaxNonMaledetto = livelliAttivi.stream()
                            .filter(x -> e.getKey().getId().toString()
                                    .equals(x.getLabel(Constants.ITEM_LABEL_CLASSE)))
                            .map(x -> parseIntOrNull(x.getLabel(Constants.ITEM_LIVELLO_LVL_CLASSE)))
                            .filter(Objects::nonNull)
                            .max(Integer::compareTo)
                            .orElse(0);

                    dto.setClasse(e.getKey());
                    dto.setLivelli(livelliSet);
                    dto.setLivelloMax(livelloMax);
                    dto.setLivelloTotale(livelloTotale);
                    dto.setLivelloNonMaledetto(livelloNonMaledetto);
                    dto.setLivelloMaxNonMaledetto(livelloMaxNonMaledetto);
                    return dto;
                })
                .sorted(Comparator.comparing(
                        x -> x.getClasse().getNome(),
                        Comparator.nullsLast(String::compareToIgnoreCase)
                ))
                .toList();

        out.setClassi(classi);
        return out;
    }

    private static Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
