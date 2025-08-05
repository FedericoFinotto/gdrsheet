package it.fin8.gdrsheet.spellparser;

import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.entity.Item;
import it.fin8.gdrsheet.entity.ItemLabel;
import it.fin8.gdrsheet.repository.ItemRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Component
public class IncantesimoParser {

    private final ItemRepository itemRepository;

    private static final Map<String, String> CLASS_MAP = Map.ofEntries(
            Map.entry("Water", "SP_WATER"),
            Map.entry("Chaos", "SP_CHAOS"),
            Map.entry("Law", "SP_LAW"),
            Map.entry("Destruction", "SP_DESTRUCTION"),
            Map.entry("Clr", "SP_CLERIC"),
            Map.entry("cleric", "SP_CLERIC"),
            Map.entry("Wiz", "SP_WIZARD"),
            Map.entry("War", "SP_WAR"),
            Map.entry("Knowledge", "SP_KNOWLEDGE"),
            Map.entry("Earth", "SP_EARTH"),
            Map.entry("Plant", "SP_PLANT"),
            Map.entry("Evil", "SP_EVIL"),
            Map.entry("Protection", "SP_PROTECTION"),
            Map.entry("Trickery", "SP_TRICKERY"),
            Map.entry("Magic", "SP_MAGIC"),
            Map.entry("Air", "SP_AIR"),
            Map.entry("Sun", "SP_SUN"),
            Map.entry("Travel", "SP_TRAVEL"),
            Map.entry("Luck", "SP_LUCK"),
            Map.entry("Brd", "SP_BARD"),
            Map.entry("Bard", "SP_BARD"),
            Map.entry("Animal", "SP_ANIMAL"),
            Map.entry("Sor", "SP_SORCERER"),
            Map.entry("sorcerer", "SP_SORCERER"),
            Map.entry("Drd", "SP_DRUID"),
            Map.entry("Druid", "SP_DRUID"),
            Map.entry("Fire", "SP_FIRE"),
            Map.entry("Death", "SP_DEATH"),
            Map.entry("Healing", "SP_HEALING"),
            Map.entry("Pal", "SP_PALADIN"),
            Map.entry("Paladin", "SP_PALADIN"),
            Map.entry("Good", "SP_GOOD"),
            Map.entry("Rgr", "SP_RANGER"),
            Map.entry("Ranger", "SP_RANGER"),
            Map.entry("ranger", "SP_SORCERER"),
            Map.entry("Strength", "SP_STRENGTH"),
            Map.entry("Force", "SP_FORCE"),
            Map.entry("Blackguard", "SP_BLACKGUARD"),
            Map.entry("blackguard", "SP_BLACKGUARD"),
            Map.entry("Glory", "SP_GLORY"),
            Map.entry("Dream", "SP_DREAM"),
            Map.entry("Mind", "SP_MIND"),
            Map.entry("Ocean", "SP_OCEAN"),
            Map.entry("Liberation", "SP_LIBERATION"),
            Map.entry("Pestilence", "SP_PESTILENCE"),
            Map.entry("Purification", "SP_PURIFICATION"),
            Map.entry("Celerity", "SP_CELERITY"),
            Map.entry("Cold", "SP_COLD"),
            Map.entry("Creation", "SP_CREATION"),
            Map.entry("Cleric", "SP_CLERIC"),
            Map.entry("Pact", "SP_PACT"),
            Map.entry("Domination", "SP_DOMINATION"),
            Map.entry("Weather", "SP_WEATHER"),
            Map.entry("druid", "SP_DRUID"),
            Map.entry("wizard", "SP_WIZARD"),
            Map.entry("Wizard", "SP_WIZARD"),
            Map.entry("paladin", "SP_PALADIN"),
            Map.entry("Joy", "SP_JOY"),
            Map.entry("Wrath", "SP_WRATH"),
            Map.entry("Herald", "SP_HERALD"),
            Map.entry("Sorcerer", "SP_SORCERER"),
            Map.entry("Pleasure", "SP_PLEASURE"),
            Map.entry("Endurance", "SP_ENDURANCE"),
            Map.entry("Sanctified", "SP_SANCTIFIED"),
            Map.entry("Fey", "SP_FEY"),
            Map.entry("Celestial", "SP_CELESTIAL"),
            Map.entry("Community", "SP_COMMUNITY"),
            Map.entry("bard", "SP_BARD"),
            Map.entry("hexblade", "SP_HEXBLADE"),
            Map.entry("Hexblade", "SP_HEXBLADE"),
            Map.entry("Assassin", "SP_ASSASIN"),
            Map.entry("Warmage", "SP_WARMAGE"),
            Map.entry("warmage", "SP_WARMAGE"),
            Map.entry("Adept", "SP_ADEPT"),
            Map.entry("shugenja", "SP_SHUGENJA"),
            Map.entry("Greed", "SP_GREED"),
            Map.entry("Asn", "SP_ASN"),
            Map.entry("Dragon", "SP_DRAGON"),
            Map.entry("Demonic", "SP_DEMONIC"),
            Map.entry("Corruption", "SP_CORRUPTION"),
            Map.entry("Temptation", "SP_TEMPTATION"),
            Map.entry("Fury", "SP_FURY"),
            Map.entry("Entropy", "SP_ENTROPY"),
            Map.entry("Ooze", "SP_OOZE"),
            Map.entry("Madness", "SP_MADNESS"),
            Map.entry("Winter", "SP_WINTER"),
            Map.entry("healer", "SP_HEALER"),
            Map.entry("oneiromancy", "SP_ONEIROMANCY"),
            Map.entry("Corrupt", "SP_CORRUPT"),
            Map.entry("Oneiromancy", "SP_ONEIROMANCY"),
            Map.entry("Spite", "SP_SPITE"),
            Map.entry("Hunger", "SP_HUNGER"),
            Map.entry("Deathbound", "SP_DEATHBOUND"),
            Map.entry("Blk", "SP_BLACKGUARD"),
            Map.entry("Wmg", "SP_WARMAGE"),
            Map.entry("Hlr", "SP_HEALER"),
            Map.entry("Duskblade", "SP_DUSKBLADE"),
            Map.entry("beguiler", "SP_BEGUILER"),
            Map.entry("Beguiler", "SP_BEGUILER"),
            Map.entry("duskblade", "SP_DUSKBLADE"),
            Map.entry("Destiny", "SP_DESTINY"),
            Map.entry("City", "SP_CITY"),
            Map.entry("Sky", "SP_SKY"),
            Map.entry("Sand", "SP_SAND"),
            Map.entry("Seafolk", "SP_SEAFOLK"),
            Map.entry("Blackwater", "SP_BLACKWATER"),
            Map.entry("Rune", "SP_RUNE"),
            Map.entry("Thirst", "SP_THIRST"),
            Map.entry("Summer", "SP_SUMMER"),
            Map.entry("Repose", "SP_REPOSE")
    );

    private static final Map<String, String> COMPONENT_MAP = Map.ofEntries(
            Map.entry("DF", "F"),
            Map.entry("XP", "XP"),
            Map.entry("XP; see text", "XP"),
            Map.entry("F", "F"),
            Map.entry("F; see text", "F"),
            Map.entry("M", "M"),
            Map.entry("M; see text", "M"),
            Map.entry("M/DF", "F"),
            Map.entry("F/DF", "F"),
            Map.entry("V", "V"),
            Map.entry("V (Brd only)", "V"),
            Map.entry("S", "S"),
            Map.entry("(F); see text", "F"),
            Map.entry("S524", "S"),
            Map.entry("Abstinence", "Abstinence"),
            Map.entry("Celestial", "Celestial"),
            Map.entry("Sacrifice", "Sacrifice"),
            Map.entry("Archon", "Archon"),
            Map.entry("S Ca sting Time: 1 swift action", "S"),
            Map.entry("M Ca sting Time: 1 standard action", "M"),
            Map.entry("S469", "S"),
            Map.entry("V549", "V"),
            Map.entry("S; Drow", "S"),
            Map.entry("DF; Drow", "F"),
            Map.entry("B", "B"),
            Map.entry("None", "None"),
            Map.entry("Good", "Good"),
            Map.entry("Evil", "Evil"),
            Map.entry("Frostfell", "Frostfell"),
            Map.entry("Coldfire", "Coldfire"),
            Map.entry("Corrupt", "Corrupt"),
            Map.entry("X", "X"),
            Map.entry("T XP", "T"),
            Map.entry("T", "T"),
            Map.entry("V459", "V"),
            Map.entry("V458", "V")
    );

    public static final Map<String, String> MAPPA_MANUALI = Map.ofEntries(
            Map.entry("Core", "Player’s Handbook (Manuale del Giocatore)"),
            Map.entry("CD", "Complete Divine"),
            Map.entry("BoED", "Book of Exalted Deeds"),
            Map.entry("CAd", "Complete Adventurer"),
            Map.entry("CAr", "Complete Arcane"),
            Map.entry("CM", "Complete Mage"),
            Map.entry("CS", "Cityscape"),
            Map.entry("City", "Cityscape"),
            Map.entry("DrM", "Dragon Magazine"),
            Map.entry("DrM(UA)", "Dragon Magazine"),
            Map.entry("Frost", "Frostburn"),
            Map.entry("MH", "Miniatures Handbook"),
            Map.entry("Planar", "Planar Handbook"),
            Map.entry("RoD", "Races of Destiny"),
            Map.entry("RotW", "Races of the Wild"),
            Map.entry("CC", "Complete Champion"),
            Map.entry("DotU", "Drow of the Underdark"),
            Map.entry("Drac", "Draconomicon"),
            Map.entry("LoM", "Lords of Madness"),
            Map.entry("HoB", "Heroes of Battle"),
            Map.entry("MoI", "Magic of Incarnum"),
            Map.entry("PHBII", "Player’s Handbook II"),
            Map.entry("HoH", "Heroes of Horror"),
            Map.entry("RoS", "Races of Stone"),
            Map.entry("LM", "Libris Mortis: The Book of Undead"),
            Map.entry("FC1", "Fiendish Codex I: Hordes of the Abyss"),
            Map.entry("ToM", "Tome of Magic"),
            Map.entry("WoL", "Weapons of Legacy"),
            Map.entry("Sand", "Sandstorm"),
            Map.entry("Storm", "Stormwrack"),
            Map.entry("CA", "Complete Scoundrel"),
            Map.entry("SRD", "Standard")
    );
    private final GoogleTranslator googleTranslator;


    public IncantesimoParser(ItemRepository itemRepository, GoogleTranslator googleTranslator) {
        this.itemRepository = itemRepository;
        this.googleTranslator = googleTranslator;
    }


    public List<Incantesimo> parse(File htmlFile) throws Exception {
        Document doc = Jsoup.parse(htmlFile, "UTF-8");
        return parseDocument(doc);
    }

    public List<Incantesimo> parseDocument(Document doc) {
        List<Incantesimo> lista = new ArrayList<>();
        Elements titoli = doc.select("h6");

        for (Element titolo : titoli) {
            Element anchor = titolo.selectFirst("a");
            String nome = anchor != null ? anchor.text().trim() : titolo.text().trim();

            // Se non ci sono stat-block dopo, salta
            if (!hasStatBlockNearby(titolo)) continue;

            Incantesimo inc = new Incantesimo();
            inc.nome = nome;

            // 1. SCUOLA → primo <p><i> subito dopo
            Element scuolaP = titolo.nextElementSibling();
            while (scuolaP != null && !(scuolaP.tagName().equals("p") && scuolaP.selectFirst("i") != null)) {
                scuolaP = scuolaP.nextElementSibling();
            }
            if (scuolaP != null) {
                inc.scuola = scuolaP.selectFirst("i").text().trim();
            }

            // 2. STATI → cerca tutti gli span dopo il titolo
            Element e = scuolaP != null ? scuolaP.nextElementSibling() : titolo.nextElementSibling();
            while (e != null && !e.tagName().equals("h6")) {
                if (e.tagName().equals("span") && e.hasClass("stat-block")) {
                    Element b = e.selectFirst("b");
                    if (b != null) {
                        String chiave = b.text().trim().toLowerCase();
                        String valore = e.ownText().replaceFirst("^:\\s*", "").trim();

                        switch (chiave) {
                            case "level" -> inc.livello = valore;
                            case "components" -> inc.componenti = valore;
                            case "casting time" -> inc.tempo = valore;
                            case "range" -> inc.range = valore;
                            case "duration" -> inc.durata = valore;
                            case "saving throw" -> inc.tiroSalvezza = valore;
                            case "spell resistance" -> inc.resistenza = valore;
                        }
                    }
                }
                e = e.nextElementSibling();
            }

            // 3. EFFETTO/DESCRIZIONE → tutti i <p> fino al prossimo <h6>
            Element current = titolo.nextElementSibling();
            StringBuilder descrizione = new StringBuilder();

            while (current != null && !current.tagName().equals("h6")) {
                if (current.tagName().equals("p")) {
                    descrizione.append(current.text().trim()).append("\n\n");
                }
                current = current.nextElementSibling();
            }

            inc.effetto = descrizione.toString().trim();
            lista.add(inc);
        }

        return lista;
    }

    public List<Item> postProcessing(List<Incantesimo> input, String manuale) {
        List<Item> spells = new ArrayList<>();
        Set<String> componentiNonMappate = new HashSet<>();
        Set<String> classiNonTrovate = new HashSet<>();

        for (Incantesimo inc : input) {
            Item spell = new Item();
            spell.setNome(capitalize(inc.nome));
            if (inc.nome == null) {
                break;
            }
            spell.setDescrizione(inc.effetto);
            spell.setTipo(TipoItem.INCANTESIMO);
            List<ItemLabel> labels = new ArrayList<>();

            if (manuale != null) {
                ItemLabel lab = new ItemLabel();
                lab.setItem(spell);
                lab.setLabel("MANUALE_SP");
                lab.setValore(MAPPA_MANUALI.get(manuale));
                if (MAPPA_MANUALI.get(manuale) == null) {
                    break;
                }
                labels.add(lab);
            }

            // scuola
            if (inc.scuola != null) {
                ItemLabel lab = new ItemLabel();
                lab.setItem(spell);
                lab.setLabel("SCUOLA_SP");
                lab.setValore(inc.scuola);
                labels.add(lab);
            }

            // componenti
            if (inc.componenti != null) {
                String[] parts = inc.componenti.split(",\\s*");
                for (String part : parts) {
                    String normalized = part.trim();
                    String mapped = COMPONENT_MAP.get(normalized);

                    if (mapped == null) {
                        componentiNonMappate.add(normalized);
                        mapped = "COMP_" + normalized.toUpperCase().replaceAll("[^A-Z]", "_");
                    }

                    ItemLabel lab = new ItemLabel();
                    lab.setItem(spell);
                    lab.setLabel("COMP_SP");
                    lab.setValore(mapped);
                    labels.add(lab);
                }
            }

            // tempo
            if (inc.tempo != null) {
                ItemLabel lab = new ItemLabel();
                lab.setItem(spell);
                lab.setLabel("TEMPO_SP");
                lab.setValore(inc.tempo);
                labels.add(lab);
            }

            // range
            if (inc.range != null) {
                ItemLabel lab = new ItemLabel();
                lab.setItem(spell);
                lab.setLabel("RANGE_SP");
                lab.setValore(inc.range);
                labels.add(lab);
            }

            // durata
            if (inc.durata != null) {
                ItemLabel lab = new ItemLabel();
                lab.setItem(spell);
                lab.setLabel("DURATA_SP");
                lab.setValore(inc.durata);
                labels.add(lab);
            }

            // tiro salvezza
            if (inc.tiroSalvezza != null) {
                ItemLabel lab = new ItemLabel();
                lab.setItem(spell);
                lab.setLabel("TS_SP");
                lab.setValore(inc.tiroSalvezza);
                labels.add(lab);
            }

            // resistenza
            if (inc.resistenza != null) {
                ItemLabel lab = new ItemLabel();
                lab.setItem(spell);
                lab.setLabel("RES_SP");
                lab.setValore(inc.resistenza);
                labels.add(lab);
            }

            // livello
            if (inc.livello != null) {
                List<String> listaLivelli = listaLivelli(inc.livello);
                for (String s : listaLivelli) {
                    String[] split = s.split(" ");
                    if (split.length != 2) continue;

                    String classe = split[0];
                    String livelloStr = split[1];
                    String classTag = CLASS_MAP.get(classe);

                    if (classTag != null) {
                        ItemLabel lab = new ItemLabel();
                        lab.setItem(spell);
                        lab.setLabel(classTag);
                        lab.setValore(livelloStr);
                        labels.add(lab);
                    } else {
                        classiNonTrovate.add(classe);
                    }
                }
            }

            spell.setLabels(labels);
            spells.add(spell);
        }

        System.out.println("Componenti non mappate: " + componentiNonMappate);
        System.out.println("Classi non trovate: " + classiNonTrovate);

        return spells;
    }


    private static List<String> listaLivelli(String lvl) {
        List<String> livelliEspansi = new ArrayList<>();

        String[] splitted = lvl.split(",\\s*");
        for (String voce : splitted) {
            voce = voce.trim();
            // Matcha ad esempio "Sor/Wiz 4"
            if (voce.matches(".*?/.*?\\s+\\d+")) {
                // Separiamo classi e livello
                String[] parti = voce.split("\\s+");
                if (parti.length == 2) {
                    String classi = parti[0];
                    String livello = parti[1];
                    String[] classiSeparate = classi.split("/");

                    for (String cls : classiSeparate) {
                        livelliEspansi.add(cls + " " + livello);
                    }
                } else {
                    livelliEspansi.add(voce);
                }
            } else {
                livelliEspansi.add(voce);
            }
        }

        return livelliEspansi;
    }


    private static boolean hasStatBlockNearby(Element titolo) {
        Element e = titolo.nextElementSibling();
        int maxCheck = 10;
        while (e != null && maxCheck-- > 0) {
            if (e.tagName().equals("span") && e.hasClass("stat-block")) return true;
            e = e.nextElementSibling();
        }
        return false;
    }


    private static String estrai(String html, String start, String end) {
        if (start.isEmpty()) {
            int br = html.indexOf("<br>");
            return br != -1 ? Jsoup.parse(html.substring(0, br)).text() : "";
        }
        int idxStart = html.indexOf(start);
        if (idxStart == -1) return "";
        idxStart += start.length();
        int idxEnd = html.indexOf(end, idxStart);
        if (idxEnd == -1) return Jsoup.parse(html.substring(idxStart)).text();
        return Jsoup.parse(html.substring(idxStart, idxEnd)).text();
    }

    public Item persist(Item i) {
        return itemRepository.save(i);
    }

    private String capitalize(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

    public List<Item> edit() throws IOException {
        List<Item> incantesimi = itemRepository.findItemsByTipo(TipoItem.INCANTESIMO);
        List<Integer> esclusi = List.of(783, 1457);
        int totale = incantesimi.stream().filter(x -> !esclusi.contains(x.getId())).toList().size();
        int counter = 0;
        for (Item item : incantesimi.stream().filter(x -> !esclusi.contains(x.getId())).toList()) {
            String descrizione = item.getDescrizione();
            String contatore = ++counter + "/" + totale;
            if (descrizione != null) {
                System.out.println(contatore + " " + item.getNome());
                // 1) Divido in due parti sul primo doppio o singolo a capo
                String[] parts = descrizione.split("\\R\\R|\\R", 2);
                System.out.println(contatore + " " + "RIMUOVO" + parts[0]);
                // parts[0] = tutto fino al primo a capo (o doppio)
                // parts[1] = il resto (se esiste), altrimenti ""
                String resto = parts.length > 1 ? parts[1] : "";
                if (parts.length > 1) {
                    // 2) Sostituisco tutti gli a capo restanti con spazi
                    String testoPulito = resto.replaceAll("\\r?\\n+", " ").trim();
                    System.out.println(contatore + " " + "MANTENGO" + testoPulito);

//                String tradotto = GoogleTranslator.translate(testoPulito, "en", "it");
//                System.out.println(tradotto);

                    // se vuoi salvare la descrizione modificata:
                    item.setDescrizione(testoPulito);
                } else {
                    System.out.println(contatore + " " + "DA ESCLUDERE");
                }


                System.out.println(contatore + " " + "Persisto");
            }
        }


        itemRepository.saveAll(incantesimi);

        return null;
    }

    private void dumpControlChars(String s) {
        // usa un LinkedHashSet per mantenere ordine di prima occorrenza
        Set<Integer> codes = new LinkedHashSet<>();
        for (int cp : s.codePoints().toArray()) {
            if (Character.isISOControl(cp) || !Character.isDefined(cp) || Character.isWhitespace(cp) && cp != ' ') {
                codes.add(cp);
            }
        }
        if (codes.isEmpty()) {
            System.out.println("Nessun carattere di controllo speciale trovato.");
        } else {
            System.out.println("Trovati caratteri speciali:");
            for (int cp : codes) {
                System.out.printf(" U+%04X (%s)%n", cp, Character.getName(cp));
            }
        }
    }


}

