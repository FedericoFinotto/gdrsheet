package it.fin8.gdrsheet.mapper;

import it.fin8.gdrsheet.config.Constants;
import it.fin8.gdrsheet.dto.*;
import it.fin8.gdrsheet.entity.Collegamento;
import it.fin8.gdrsheet.entity.Item;
import it.fin8.gdrsheet.repository.ItemRepository;
import it.fin8.gdrsheet.service.UtilService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ItemMapper {

    private final UtilService utilService;
    private final ItemRepository itemRepository;

    public ItemMapper(UtilService utilService, ItemRepository itemRepository) {
        this.utilService = utilService;
        this.itemRepository = itemRepository;
    }

    public ItemDTO toDTO(Item entity) {
        return toDTO(entity, null, null);
    }

    public ItemDTO toDTO(Item entity, Integer utilizziTotale, Integer utilizziUsati) {
        ItemDTO dto = new ItemDTO();
        populateBase(dto, entity, utilizziTotale, utilizziUsati);
        return dto;
    }

    /** FRUTTO: stessi campi di {@link #toDTO}, più il campo trasformazioni (valorizzato dal chiamante). */
    public FruttoDTO toFruttoDTO(Item entity, Integer utilizziTotale, Integer utilizziUsati) {
        FruttoDTO dto = new FruttoDTO();
        populateBase(dto, entity, utilizziTotale, utilizziUsati);
        dto.setTrasformazioni(new ArrayList<>());
        return dto;
    }

    private void populateBase(ItemDTO dto, Item entity, Integer utilizziTotale, Integer utilizziUsati) {
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setTipo(entity.getTipo());
        dto.setDisabled(isDisabled(entity));
        dto.setQuantita(parseQuantita(entity.getLabel(Constants.LABEL_QTA)));

        if (Constants.ITEM_TIPO_BARRIERA.equalsIgnoreCase(entity.getLabel(Constants.ITEM_LABEL_TIPO))) {
            dto.setBarriera(true);
            dto.setBarrMax(parseIntOrZero(entity.getLabel(Constants.ITEM_LABEL_BARR_MAX)));
            dto.setBarrCons(parseIntOrZero(entity.getLabel(Constants.ITEM_LABEL_BARR_CONS)));
        }

        // Utilizzi: totale dall'item (globale), usati per-personaggio
        Integer tot = utilizziTotale != null ? utilizziTotale
                : parseIntOrNull(entity.getLabel(Constants.LABEL_UTILIZZI));
        if (tot != null) {
            dto.setUtilizziTotale(tot);
            dto.setUtilizziUsati(utilizziUsati != null ? utilizziUsati : 0);
        }
        // Peso, capienza e flag armi (solo su CONTENITORE)
        String pesoStr = entity.getLabel(Constants.LABEL_PESO);
        if (pesoStr != null) dto.setPeso(parseDouble(pesoStr));
        String capienzaStr = entity.getLabel(Constants.LABEL_CAPIENZA);
        if (capienzaStr != null) dto.setCapienza(parseDouble(capienzaStr));
        if ("1".equals(entity.getLabel(Constants.LABEL_INCLUDI_ARMI_ABILITATE)))
            dto.setIncludiArmiAbilitate(true);
        if ("1".equals(entity.getLabel(Constants.LABEL_INCLUDI_OGGETTI_ABILITATI)))
            dto.setIncludiOggettiAbilitati(true);
        if ("1".equals(entity.getLabel(Constants.LABEL_INCLUDI_CONSUMABILI_ABILITATI)))
            dto.setIncludiConsumabiliAbilitati(true);
        if ("1".equals(entity.getLabel(Constants.LABEL_INCLUDI_TUTTI_ABILITATI)))
            dto.setIncludiTuttiAbilitati(true);
    }

    private static Integer parseQuantita(String s) {
        if (s == null) return 1;
        try {
            return Math.max(0, Integer.parseInt(s.trim()));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private static Double parseDouble(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Double.parseDouble(s.trim().replace(',', '.')); } catch (NumberFormatException e) { return null; }
    }

    private static Integer parseIntOrZero(String s) {
        if (s == null) return 0;
        try {
            return Math.max(0, Integer.parseInt(s.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public LivelloDTO toLivelloDTO(Item entity) {
        LivelloDTO dto = new LivelloDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setTipo(entity.getTipo());
        dto.setDisabled(isDisabled(entity));
        dto.setLivello(Integer.parseInt(entity.getLabel(Constants.ITEM_LIVELLO_LVL)));
        String classeString = entity.getLabel(Constants.ITEM_LABEL_CLASSE);
        String maledizioneString = entity.getLabel(Constants.ITEM_LABEL_MALEDIZIONE);

        if (classeString != null) {
            Integer classeId = Integer.valueOf(classeString);
            Item classeItem = itemRepository.findItemById(classeId);
            dto.setClasse(classeItem.getNome());
            dto.setClasseId(classeId);
        }
        if (maledizioneString != null) {
            dto.setMaledizione(maledizioneString);
        }
        dto.setLivelliClasse(utilService.parseStringToIntList(entity.getLabel(Constants.ITEM_LIVELLO_LVL_CLASSE)));
        String gradiString = entity.getLabel(Constants.ITEM_LABEL_GRADI_LIVELLO);
        if (gradiString != null && !gradiString.isBlank()) {
            try {
                dto.setGradi(Integer.valueOf(gradiString.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return dto;
    }

    public SpellBookIncantesimoDTO toIncantesimoDTO(Item classe, ItemLivelloDTO itemLivelloDTO) {
        Item entity = itemLivelloDTO.getItem();
        SpellBookIncantesimoDTO dto = new SpellBookIncantesimoDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setTipo(entity.getTipo());
        dto.setLivello(Integer.parseInt(itemLivelloDTO.getLivello()));
        dto.setIdClasse(classe.getId());
        dto.setClasse(classe.getNome());
        dto.setSpellList(utilService.getItemLabel(classe, Constants.ITEM_LABEL_LISTA_INCANTESIMI));
        dto.setComponenti(utilService.getItemLabels(entity, Constants.ITEM_LABEL_COMPONENTE));
        dto.setScuola(utilService.getItemLabel(entity, Constants.ITEM_LABEL_SCUOLA_SP));
        return dto;
    }

    public SpellBookIncantesimoDTO toIncantesimoDTO(Item classe, Collegamento coll) {
        Item entity = coll.getItemTarget();
        boolean alwaysPrep = Objects.equals(coll.getLabel(Constants.COLLEGAMENTO_LABEL_N_PREPARATI), Constants.COLLEGAMENTO_LABEL_ALWAYS_PREPARED);
        SpellBookIncantesimoDTO dto = new SpellBookIncantesimoDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setTipo(entity.getTipo());
        dto.setLivello(Integer.parseInt(utilService.getCollegamentoLabel(coll, Constants.COLLEGAMENTO_LABEL_LIVELLO)));
        dto.setIdClasse(classe.getId());
        dto.setClasse(classe.getNome());
        dto.setSpellList(utilService.getCollegamentoLabel(coll, Constants.COLLEGAMENTO_LABEL_LISTA_INCANTESIMI));
        dto.setComponenti(utilService.getItemLabels(coll.getItemTarget(), Constants.ITEM_LABEL_COMPONENTE));
        if (alwaysPrep) {
            dto.setAlwaysPrep(true);
            dto.setNPrepared(null);
            dto.setNUsed(null);
        } else {
            dto.setAlwaysPrep(false);
            dto.setNPrepared(Integer.parseInt(utilService.getCollegamentoLabel(coll, Constants.COLLEGAMENTO_LABEL_N_PREPARATI)));
            dto.setNUsed(Integer.parseInt(utilService.getCollegamentoLabel(coll, Constants.COLLEGAMENTO_LABEL_N_USATI)));
        }
        return dto;
    }

    public AttaccoDTO toAttaccoDTO(Item entity) {
        List<Collegamento> parents = entity.getParent();
        String nomeItemParent = null;
        if (parents != null && !parents.isEmpty()) {
            Item parent = parents.get(0).getItemSource();
            nomeItemParent = parent.getNome();
        }
        AttaccoDTO dto = new AttaccoDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setTipo(entity.getTipo());
        dto.setNomeItem(nomeItemParent);
        dto.setAttacco(entity.getLabel(Constants.ITEM_LABEL_ATTACCO_TIRO_PER_COLPIRE));
        dto.setColpo(entity.getLabel(Constants.ITEM_LABEL_ATTACCO_DANNI));
        dto.setTiroSalvezza(entity.getLabel(Constants.ITEM_LABEL_ATTACCO_TIRO_SALVEZZA));
        dto.setTipoDanno(entity.getLabel(Constants.ITEM_LABEL_ATTACCO_TIPO_DANNI));
        dto.setRange(entity.getLabel(Constants.ITEM_LABEL_ATTACCO_RANGE));

        return dto;
    }

    public Boolean isDisabled(Item entity) {
        String disabledLabel = entity.getLabel(Constants.ITEM_LABEL_DISABILITATO);
        return disabledLabel != null && disabledLabel.equals("1");
    }

    public ClasseDTO toClasseDTO(InfoClasseDTO classe) {
        Item entity = classe.getClasse();
        ClasseDTO dto = new ClasseDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setTipo(entity.getTipo());
        dto.setLivelli(classe.getLivelli().stream().toList());
        dto.setSpell(utilService.getItemLabel(entity, Constants.ITEM_LABEL_LISTA_INCANTESIMI));
        return dto;
    }

    public Collegamento toNewCollegamentoIncantesimo(Map.Entry<Integer, Integer> sp, Integer sourceId, String livello, String spellList, EntityManager em) {
        Integer targetId = sp.getKey();
        Collegamento col = new Collegamento();
        col.setItemSource(em.getReference(Item.class, sourceId));
        col.setItemTarget(em.getReference(Item.class, targetId));
        col.setLabels(new ArrayList<>());
        col.setLabel(Constants.COLLEGAMENTO_LABEL_LIVELLO, livello);
        col.setLabel(Constants.COLLEGAMENTO_LABEL_LISTA_INCANTESIMI, spellList);
        if (sp.getValue() == -54) {
            col.setLabel(Constants.COLLEGAMENTO_LABEL_N_PREPARATI, "ALWAYS");
        } else {
            col.setLabel(Constants.COLLEGAMENTO_LABEL_N_PREPARATI, sp.getValue().toString());
            col.setLabel(Constants.COLLEGAMENTO_LABEL_N_USATI, "0");
        }
        return col;
    }

    public TrasformazioneDTO toTrasformazioneDTO(Item entity) {
        TrasformazioneDTO dto = new TrasformazioneDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setTipo(entity.getTipo());
        dto.setDisabled(isDisabled(entity));
        dto.setGruppo(entity.getLabel(Constants.ITEM_LABEL_GRUPPO_TRASFORMAZIONE));
        return dto;
    }
}
