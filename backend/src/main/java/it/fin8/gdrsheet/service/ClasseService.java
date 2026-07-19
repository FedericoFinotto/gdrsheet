package it.fin8.gdrsheet.service;

import it.fin8.gdrsheet.config.Constants;
import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.def.TipoModificatore;
import it.fin8.gdrsheet.dto.ClasseDetailDTO;
import it.fin8.gdrsheet.entity.*;
import it.fin8.gdrsheet.repository.AvanzamentoRepository;
import it.fin8.gdrsheet.repository.ItemRepository;
import it.fin8.gdrsheet.repository.ModificatoreRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Lettura e salvataggio completo delle CLASSI:
 * item CLASSE (label ABCLASSE/SPELL/SP_SLOT_BONUS) + 20 item AVANZAMENTO
 * "NOME N" (label SP_SLOT/SPELL, modificatori BAB/TMP/RFL/VLT/DV/GRADI)
 * + abilità concesse per livello (righe avanzamento verso item generici).
 */
@Service
public class ClasseService {

    private static final int LIVELLI = 20;
    private static final List<String> STAT_LIVELLO = List.of("BAB", "TMP", "RFL", "VLT");

    private final ItemRepository itemRepository;
    private final AvanzamentoRepository avanzamentoRepository;
    private final ModificatoreRepository modificatoreRepository;
    private final EntityManager em;

    private final PersonaggioCacheService personaggioCacheService;

    public ClasseService(ItemRepository itemRepository,
                         AvanzamentoRepository avanzamentoRepository,
                         ModificatoreRepository modificatoreRepository,
                         EntityManager em,
                         PersonaggioCacheService personaggioCacheService) {
        this.itemRepository = itemRepository;
        this.avanzamentoRepository = avanzamentoRepository;
        this.modificatoreRepository = modificatoreRepository;
        this.em = em;
        this.personaggioCacheService = personaggioCacheService;
    }

    public ClasseDetailDTO getClasse(Integer id) {
        Item classe = itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe non trovata"));
        if (!TipoItem.CLASSE.equals(classe.getTipo()) && !TipoItem.RAZZA.equals(classe.getTipo()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'item non è una classe o razza");

        ClasseDetailDTO dto = new ClasseDetailDTO();
        dto.setId(classe.getId());
        dto.setTipo(classe.getTipo().name());
        dto.setNome(classe.getNome());
        dto.setEnName(classe.getLabel(Constants.ITEM_LABEL_EN_NAME));
        dto.setManuale(classe.getLabel(Constants.ITEM_LABEL_MANUALE));
        dto.setDescrizione(classe.getDescrizione());
        dto.setRazzaTaglia(classe.getLabel(Constants.ITEM_LABEL_RAZZA_TAGLIA));
        dto.setRazzaVelocita(classe.getLabel(Constants.ITEM_LABEL_RAZZA_VELOCITA));
        dto.setRazzaCaratteristiche(classe.getLabel(Constants.ITEM_LABEL_RAZZA_CARATTERISTICHE));
        dto.setRazzaLap(classe.getLabel(Constants.ITEM_LABEL_RAZZA_LAP));
        dto.setRazzaSpazio(classe.getLabel(Constants.ITEM_LABEL_RAZZA_SPAZIO));
        dto.setRazzaPortata(classe.getLabel(Constants.ITEM_LABEL_RAZZA_PORTATA));

        String abClasse = classe.getLabel(Constants.ITEM_LABEL_ABILITA_CLASSE);
        dto.setAbilitaClasse(abClasse == null || abClasse.isBlank()
                ? List.of()
                : Arrays.stream(abClasse.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        dto.setSpellList(classe.getLabel(Constants.ITEM_LABEL_LISTA_INCANTESIMI));
        dto.setSpellSlotBonus(classe.getLabel(Constants.ITEM_LABEL_SPELL_SLOT_BONUS));
        dto.setSezioniIncantesimi(buildSezioni(classe));
        dto.setRank1(classe.getLabel(Constants.ITEM_LABEL_RANK_PRIMO));
        dto.setRank(classe.getLabel(Constants.ITEM_LABEL_RANK));
        int numLivelli = parseNumLivelli(classe.getLabel(Constants.ITEM_LABEL_NUM_LIVELLI_CLASSE));
        dto.setNumLivelli(numLivelli);
        dto.setDv(classe.getLabel(Constants.ITEM_LABEL_DADI_VITA));
        dto.setIdMondo(classe.getMondo() != null ? classe.getMondo().getId() : null);
        dto.setIdSistema(classe.getSistema() != null ? classe.getSistema().getId() : null);

        List<Avanzamento> avanzamenti = avanzamentoRepository.findAllByItemSource_Id(id);

        Map<Integer, Item> avzPerLivello = new HashMap<>();
        List<ClasseDetailDTO.AbilitaConcessaDTO> concesse = new ArrayList<>();
        for (Avanzamento av : avanzamenti) {
            Item target = av.getItemTarget();
            if (TipoItem.AVANZAMENTO.equals(target.getTipo())) {
                avzPerLivello.putIfAbsent(av.getLivello(), target);
            } else {
                concesse.add(new ClasseDetailDTO.AbilitaConcessaDTO(
                        av.getLivello(), target.getId(), target.getNome(),
                        target.getTipo() != null ? target.getTipo().name() : null, av.getQty()));
            }
        }
        concesse.sort(Comparator.comparingInt(ClasseDetailDTO.AbilitaConcessaDTO::getLivello)
                .thenComparing(a -> String.valueOf(a.getNome()), String.CASE_INSENSITIVE_ORDER));
        dto.setAbilitaConcesse(concesse);

        List<ClasseDetailDTO.LivelloClasseDTO> livelli = new ArrayList<>();
        for (int l = 1; l <= numLivelli; l++) {
            Item avz = avzPerLivello.get(l);
            ClasseDetailDTO.LivelloClasseDTO row = new ClasseDetailDTO.LivelloClasseDTO();
            row.setLivello(l);
            if (avz != null) {
                row.setBab(valoreMod(avz, "BAB"));
                row.setTmp(valoreMod(avz, "TMP"));
                row.setRfl(valoreMod(avz, "RFL"));
                row.setVlt(valoreMod(avz, "VLT"));
                row.setSpSlot(avz.getLabel(Constants.ITEM_LABEL_SPELL_SLOT));
            }
            livelli.add(row);
        }
        dto.setLivelli(livelli);

        return dto;
    }

    /** Costruisce le sezioni incantatore dalle label SPELL_&lt;n&gt; o, in fallback, dalla SPELL singola. */
    private List<ClasseDetailDTO.SezioneSpellDTO> buildSezioni(Item classe) {
        List<ClasseDetailDTO.SezioneSpellDTO> out = new ArrayList<>();
        java.util.TreeSet<Integer> idx = new java.util.TreeSet<>();
        if (classe.getLabels() != null) {
            for (ItemLabel l : classe.getLabels()) {
                String k = l.getLabel();
                if (k != null && k.startsWith("SPELL_")) {
                    String rest = k.substring("SPELL_".length());
                    if (rest.matches("\\d+")) idx.add(Integer.parseInt(rest));
                }
            }
        }
        for (Integer n : idx) {
            String liste = classe.getLabel("SPELL_" + n);
            if (liste == null || liste.isBlank()) continue;
            ClasseDetailDTO.SezioneSpellDTO s = new ClasseDetailDTO.SezioneSpellDTO();
            s.setListe(Arrays.stream(liste.split(",")).map(String::trim).filter(x -> !x.isEmpty()).toList());
            s.setProgressione(classe.getLabel("SPELL_" + n + "_PROG"));
            s.setBonus(classe.getLabel("SPELL_" + n + "_BONUS"));
            String slotRaw = classe.getLabel("SPELL_" + n + "_SLOT");
            s.setSlot(slotRaw == null || slotRaw.isBlank()
                    ? List.of()
                    : Arrays.stream(slotRaw.split(";")).map(String::trim).toList());
            out.add(s);
        }
        if (out.isEmpty()) {
            String legacy = classe.getLabel(Constants.ITEM_LABEL_LISTA_INCANTESIMI);
            if (legacy != null && !legacy.isBlank()) {
                ClasseDetailDTO.SezioneSpellDTO s = new ClasseDetailDTO.SezioneSpellDTO();
                s.setListe(List.of(legacy.trim()));
                s.setProgressione(it.fin8.gdrsheet.def.ProgressioneIncantesimi.CUSTOM);
                s.setBonus(classe.getLabel(Constants.ITEM_LABEL_SPELL_SLOT_BONUS));
                out.add(s);
            }
        }
        return out;
    }

    @Transactional
    public Item saveClasse(ClasseDetailDTO dto) {
        if (dto.getNome() == null || dto.getNome().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome obbligatorio");
        String nome = dto.getNome().trim();

        Item classe;
        if (dto.getId() != null) {
            classe = itemRepository.findById(dto.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe non trovata"));
            if (!TipoItem.CLASSE.equals(classe.getTipo()) && !TipoItem.RAZZA.equals(classe.getTipo()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'item non è una classe o razza");
        } else {
            classe = new Item();
            // tipo CLASSE o RAZZA (gestiti con lo stesso editor); default CLASSE
            classe.setTipo(TipoItem.RAZZA.name().equalsIgnoreCase(dto.getTipo()) ? TipoItem.RAZZA : TipoItem.CLASSE);
            classe.setLabels(new ArrayList<>());
        }
        // mondo/sistema aggiornabili sia in creazione sia in modifica
        if (dto.getIdMondo() != null) {
            it.fin8.gdrsheet.entity.Mondo m = em.find(it.fin8.gdrsheet.entity.Mondo.class, dto.getIdMondo());
            if (m != null) classe.setMondo(m);
        }
        if (dto.getIdSistema() != null) {
            it.fin8.gdrsheet.entity.Sistema s = em.find(it.fin8.gdrsheet.entity.Sistema.class, dto.getIdSistema());
            if (s != null) classe.setSistema(s);
        }
        classe.setNome(nome);
        classe.setDescrizione(dto.getDescrizione());
        putSingleLabel(classe, Constants.ITEM_LABEL_EN_NAME, dto.getEnName());
        putSingleLabel(classe, Constants.ITEM_LABEL_MANUALE, dto.getManuale());
        putSingleLabel(classe, Constants.ITEM_LABEL_RAZZA_TAGLIA, dto.getRazzaTaglia());
        putSingleLabel(classe, Constants.ITEM_LABEL_RAZZA_VELOCITA, dto.getRazzaVelocita());
        putSingleLabel(classe, Constants.ITEM_LABEL_RAZZA_CARATTERISTICHE, dto.getRazzaCaratteristiche());
        putSingleLabel(classe, Constants.ITEM_LABEL_RAZZA_LAP, dto.getRazzaLap());
        putSingleLabel(classe, Constants.ITEM_LABEL_RAZZA_SPAZIO, dto.getRazzaSpazio());
        putSingleLabel(classe, Constants.ITEM_LABEL_RAZZA_PORTATA, dto.getRazzaPortata());

        String abClasse = dto.getAbilitaClasse() == null || dto.getAbilitaClasse().isEmpty()
                ? null
                : dto.getAbilitaClasse().stream().map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
        putSingleLabel(classe, Constants.ITEM_LABEL_ABILITA_CLASSE, abClasse);

        // --- sezioni incantatore: pulizia label vecchie e riscrittura SPELL_<n> ---
        if (classe.getLabels() != null) {
            classe.getLabels().removeIf(l -> {
                String k = l.getLabel();
                return k != null && (k.equals(Constants.ITEM_LABEL_LISTA_INCANTESIMI)
                        || k.equals(Constants.ITEM_LABEL_SPELL_SLOT_BONUS)
                        || k.matches("SPELL_\\d+(_PROG|_BONUS|_SLOT)?"));
            });
        }
        List<ClasseDetailDTO.SezioneSpellDTO> sezioni = dto.getSezioniIncantesimi();
        if ((sezioni == null || sezioni.isEmpty()) && dto.getSpellList() != null && !dto.getSpellList().isBlank()) {
            ClasseDetailDTO.SezioneSpellDTO s = new ClasseDetailDTO.SezioneSpellDTO();
            s.setListe(List.of(dto.getSpellList().trim()));
            s.setProgressione(it.fin8.gdrsheet.def.ProgressioneIncantesimi.CUSTOM);
            s.setBonus(dto.getSpellSlotBonus());
            sezioni = new ArrayList<>(List.of(s));
        }
        String representativeList = null;
        if (sezioni != null) {
            int n = 0;
            for (ClasseDetailDTO.SezioneSpellDTO s : sezioni) {
                if (s.getListe() == null) continue;
                List<String> liste = s.getListe().stream().map(String::trim).filter(x -> !x.isEmpty()).toList();
                if (liste.isEmpty()) continue;
                putSingleLabel(classe, "SPELL_" + n, String.join(",", liste));
                putSingleLabel(classe, "SPELL_" + n + "_PROG", s.getProgressione());
                putSingleLabel(classe, "SPELL_" + n + "_BONUS", s.getBonus());
                // tabella slot custom della sezione (solo se CUSTOM e valorizzata)
                if (s.getSlot() != null && !s.getSlot().isEmpty()) {
                    String slotJoined = s.getSlot().stream()
                            .map(x -> x == null ? "" : x.trim())
                            .collect(Collectors.joining(";"));
                    if (!slotJoined.replace(";", "").isBlank()) {
                        putSingleLabel(classe, "SPELL_" + n + "_SLOT", slotJoined);
                    }
                }
                if (representativeList == null) representativeList = liste.get(0);
                n++;
            }
        }
        // SPELL singola legacy: prima lista, per i lettori legacy (mapper/find incantesimi)
        putSingleLabel(classe, Constants.ITEM_LABEL_LISTA_INCANTESIMI, representativeList);
        putSingleLabel(classe, Constants.ITEM_LABEL_RANK_PRIMO, dto.getRank1());
        putSingleLabel(classe, Constants.ITEM_LABEL_RANK, dto.getRank());

        int numLivelli = parseNumLivelli(dto.getNumLivelli() == null ? null : String.valueOf(dto.getNumLivelli()));
        putSingleLabel(classe, Constants.ITEM_LABEL_NUM_LIVELLI_CLASSE, String.valueOf(numLivelli));
        putSingleLabel(classe, Constants.ITEM_LABEL_DADI_VITA, dto.getDv());

        classe = itemRepository.save(classe);

        // --- avanzamenti di livello (item AVANZAMENTO "NOME N") ---
        List<Avanzamento> esistenti = avanzamentoRepository.findAllByItemSource_Id(classe.getId());
        Map<Integer, Item> avzPerLivello = new HashMap<>();
        for (Avanzamento av : esistenti) {
            if (TipoItem.AVANZAMENTO.equals(av.getItemTarget().getTipo())) {
                avzPerLivello.putIfAbsent(av.getLivello(), av.getItemTarget());
            }
        }

        if (dto.getLivelli() != null) {
            for (ClasseDetailDTO.LivelloClasseDTO row : dto.getLivelli()) {
                int l = row.getLivello();
                if (l < 1 || l > numLivelli) continue;

                Item avz = avzPerLivello.get(l);
                if (avz == null) {
                    avz = new Item();
                    avz.setTipo(TipoItem.AVANZAMENTO);
                    avz.setLabels(new ArrayList<>());
                    avz.setSistema(classe.getSistema());
                    avz.setMondo(classe.getMondo());
                    avz.setNome(nome.toUpperCase(Locale.ROOT) + " " + l);
                    avz = itemRepository.save(avz);

                    Avanzamento link = new Avanzamento();
                    link.setItemSource(classe);
                    link.setItemTarget(avz);
                    link.setLivello(l);
                    avanzamentoRepository.save(link);
                    avzPerLivello.put(l, avz);
                } else {
                    // tiene il nome allineato a quello della classe
                    avz.setNome(nome.toUpperCase(Locale.ROOT) + " " + l);
                }

                setModValore(avz, "BAB", row.getBab());
                setModValore(avz, "TMP", row.getTmp());
                setModValore(avz, "RFL", row.getRfl());
                setModValore(avz, "VLT", row.getVlt());
                setModValore(avz, "DV", null);    // DV ora gestito a livello di classe e congelato sul livello
                setModValore(avz, "GRADI", null); // gradi ora gestiti da RANK_1/RANK sulla classe: pulizia vecchio mod

                putSingleLabel(avz, Constants.ITEM_LABEL_SPELL_SLOT, row.getSpSlot());
                putSingleLabel(avz, Constants.ITEM_LABEL_LISTA_INCANTESIMI,
                        row.getSpSlot() != null && !row.getSpSlot().isBlank() ? representativeList : null);
                itemRepository.save(avz);
            }
        }

        // --- abilità concesse (righe avanzamento verso item generici) ---
        if (dto.getAbilitaConcesse() != null) {
            record Chiave(int livello, int itemId) {}
            int maxLiv = numLivelli;
            // risolvi gli itemId: gli oggetti "nuovi" (itemId null + nome) vengono creati come PRIVILEGIO
            Map<Chiave, Integer> desiderate = new LinkedHashMap<>(); // chiave -> qty
            for (ClasseDetailDTO.AbilitaConcessaDTO a : dto.getAbilitaConcesse()) {
                if (a.getLivello() < 1 || a.getLivello() > maxLiv) continue;
                Integer itemId = a.getItemId();
                if (itemId == null) {
                    if (a.getNome() == null || a.getNome().isBlank()) continue;
                    Item nuovo = new Item();
                    nuovo.setTipo(TipoItem.PRIVILEGIO);
                    nuovo.setNome(a.getNome().trim());
                    nuovo.setLabels(new ArrayList<>());
                    nuovo.setMondo(classe.getMondo());
                    nuovo.setSistema(classe.getSistema());
                    nuovo = itemRepository.save(nuovo);
                    itemId = nuovo.getId();
                }
                desiderate.put(new Chiave(a.getLivello(), itemId), a.getQty());
            }

            Map<Chiave, Avanzamento> presenti = new HashMap<>();
            for (Avanzamento av : esistenti) {
                if (TipoItem.AVANZAMENTO.equals(av.getItemTarget().getTipo())) continue;
                Chiave k = new Chiave(av.getLivello(), av.getItemTarget().getId());
                if (!desiderate.containsKey(k)) {
                    avanzamentoRepository.delete(av);
                } else {
                    presenti.put(k, av);
                }
            }
            for (Map.Entry<Chiave, Integer> entry : desiderate.entrySet()) {
                Chiave k = entry.getKey();
                Integer qty = entry.getValue();
                if (presenti.containsKey(k)) {
                    Avanzamento av = presenti.get(k);
                    if (!Objects.equals(av.getQty(), qty)) {
                        av.setQty(qty);
                        avanzamentoRepository.save(av);
                    }
                } else {
                    Item target = itemRepository.findById(k.itemId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "Item concesso non trovato: " + k.itemId()));
                    Avanzamento link = new Avanzamento();
                    link.setItemSource(classe);
                    link.setItemTarget(target);
                    link.setLivello(k.livello());
                    link.setQty(qty);
                    avanzamentoRepository.save(link);
                }
            }
        }

        // una classe/razza è condivisa da tutti i personaggi che l'hanno scelta (via label
        // CLASSE su un LIVELLO): invalida la cache di ognuno di loro.
        personaggioCacheService.invalidaPerClasse(classe.getId());

        return classe;
    }

    /**
     * Numero di livelli della classe, clampato a [1, 40]. Default 20 se assente/non valido.
     */
    private int parseNumLivelli(String raw) {
        if (raw == null || raw.isBlank()) return LIVELLI;
        try {
            return Math.max(1, Math.min(100, Integer.parseInt(raw.trim())));
        } catch (NumberFormatException e) {
            return LIVELLI;
        }
    }

    private static String valoreMod(Item item, String statId) {
        Modificatore m = item.getModificatore(statId);
        return m != null ? m.getValore() : null;
    }

    /**
     * Crea/aggiorna/elimina il modificatore VALORE sempre attivo per la stat.
     */
    private void setModValore(Item item, String statId, String valore) {
        Modificatore m = item.getModificatore(statId);
        if (valore == null || valore.isBlank()) {
            if (m != null) modificatoreRepository.delete(m);
            return;
        }
        if (m == null) {
            Stat stat = em.find(Stat.class, statId);
            if (stat == null) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Stat mancante: " + statId);
            m = new Modificatore();
            m.setItem(item);
            m.setStat(stat);
            m.setTipo(TipoModificatore.VALORE);
            m.setSempreAttivo(true);
        }
        m.setValore(valore.trim());
        modificatoreRepository.save(m);
    }

    private void putSingleLabel(Item item, String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            item.removeLabel(key);
        } else {
            item.setLabel(key, value.trim());
        }
    }
}
