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

    public ClasseService(ItemRepository itemRepository,
                         AvanzamentoRepository avanzamentoRepository,
                         ModificatoreRepository modificatoreRepository,
                         EntityManager em) {
        this.itemRepository = itemRepository;
        this.avanzamentoRepository = avanzamentoRepository;
        this.modificatoreRepository = modificatoreRepository;
        this.em = em;
    }

    public ClasseDetailDTO getClasse(Integer id) {
        Item classe = itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe non trovata"));
        if (!TipoItem.CLASSE.equals(classe.getTipo()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'item non è una classe");

        ClasseDetailDTO dto = new ClasseDetailDTO();
        dto.setId(classe.getId());
        dto.setNome(classe.getNome());
        dto.setEnName(classe.getLabel(Constants.ITEM_LABEL_EN_NAME));
        dto.setManuale(classe.getLabel(Constants.ITEM_LABEL_MANUALE));
        dto.setDescrizione(classe.getDescrizione());

        String abClasse = classe.getLabel(Constants.ITEM_LABEL_ABILITA_CLASSE);
        dto.setAbilitaClasse(abClasse == null || abClasse.isBlank()
                ? List.of()
                : Arrays.stream(abClasse.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        dto.setSpellList(classe.getLabel(Constants.ITEM_LABEL_LISTA_INCANTESIMI));
        dto.setSpellSlotBonus(classe.getLabel(Constants.ITEM_LABEL_SPELL_SLOT_BONUS));
        dto.setRank1(classe.getLabel(Constants.ITEM_LABEL_RANK_PRIMO));
        dto.setRank(classe.getLabel(Constants.ITEM_LABEL_RANK));
        int numLivelli = parseNumLivelli(classe.getLabel(Constants.ITEM_LABEL_NUM_LIVELLI_CLASSE));
        dto.setNumLivelli(numLivelli);
        dto.setDv(classe.getLabel(Constants.ITEM_LABEL_DADI_VITA));

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
                        target.getTipo() != null ? target.getTipo().name() : null));
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

    @Transactional
    public Item saveClasse(ClasseDetailDTO dto) {
        if (dto.getNome() == null || dto.getNome().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome obbligatorio");
        String nome = dto.getNome().trim();

        Item classe;
        if (dto.getId() != null) {
            classe = itemRepository.findById(dto.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Classe non trovata"));
            if (!TipoItem.CLASSE.equals(classe.getTipo()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'item non è una classe");
        } else {
            classe = new Item();
            classe.setTipo(TipoItem.CLASSE);
            classe.setLabels(new ArrayList<>());
            // mondo/sistema solo alla creazione
            if (dto.getIdMondo() != null) classe.setMondo(em.find(it.fin8.gdrsheet.entity.Mondo.class, dto.getIdMondo()));
            if (dto.getIdSistema() != null) classe.setSistema(em.find(it.fin8.gdrsheet.entity.Sistema.class, dto.getIdSistema()));
        }
        classe.setNome(nome);
        classe.setDescrizione(dto.getDescrizione());
        putSingleLabel(classe, Constants.ITEM_LABEL_EN_NAME, dto.getEnName());
        putSingleLabel(classe, Constants.ITEM_LABEL_MANUALE, dto.getManuale());

        String abClasse = dto.getAbilitaClasse() == null || dto.getAbilitaClasse().isEmpty()
                ? null
                : dto.getAbilitaClasse().stream().map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
        putSingleLabel(classe, Constants.ITEM_LABEL_ABILITA_CLASSE, abClasse);
        putSingleLabel(classe, Constants.ITEM_LABEL_LISTA_INCANTESIMI, dto.getSpellList());
        putSingleLabel(classe, Constants.ITEM_LABEL_SPELL_SLOT_BONUS, dto.getSpellSlotBonus());
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
                        row.getSpSlot() != null && !row.getSpSlot().isBlank() ? dto.getSpellList() : null);
                itemRepository.save(avz);
            }
        }

        // --- abilità concesse (righe avanzamento verso item generici) ---
        if (dto.getAbilitaConcesse() != null) {
            record Chiave(int livello, int itemId) {
            }
            int maxLiv = numLivelli;
            Set<Chiave> desiderate = dto.getAbilitaConcesse().stream()
                    .filter(a -> a.getItemId() != null && a.getLivello() >= 1 && a.getLivello() <= maxLiv)
                    .map(a -> new Chiave(a.getLivello(), a.getItemId()))
                    .collect(Collectors.toSet());

            Set<Chiave> presenti = new HashSet<>();
            for (Avanzamento av : esistenti) {
                if (TipoItem.AVANZAMENTO.equals(av.getItemTarget().getTipo())) continue;
                Chiave k = new Chiave(av.getLivello(), av.getItemTarget().getId());
                if (!desiderate.contains(k)) {
                    avanzamentoRepository.delete(av);
                } else {
                    presenti.add(k);
                }
            }
            for (Chiave k : desiderate) {
                if (presenti.contains(k)) continue;
                Item target = itemRepository.findById(k.itemId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Item concesso non trovato: " + k.itemId()));
                Avanzamento link = new Avanzamento();
                link.setItemSource(classe);
                link.setItemTarget(target);
                link.setLivello(k.livello());
                avanzamentoRepository.save(link);
            }
        }

        return classe;
    }

    /**
     * Numero di livelli della classe, clampato a [1, 40]. Default 20 se assente/non valido.
     */
    private int parseNumLivelli(String raw) {
        if (raw == null || raw.isBlank()) return LIVELLI;
        try {
            return Math.max(1, Math.min(40, Integer.parseInt(raw.trim())));
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
