package it.fin8.gdrsheet.mapper;

import it.fin8.gdrsheet.config.Constants;
import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.dto.*;
import it.fin8.gdrsheet.entity.Collegamento;
import it.fin8.gdrsheet.entity.Item;
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

    public ItemMapper(UtilService utilService) {
        this.utilService = utilService;
    }

    public ItemDTO toDTO(Item entity) {
        ItemDTO dto = new ItemDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setTipo(entity.getTipo());
        dto.setDisabled(isDisabled(entity));
        return dto;
    }

    public LivelloDTO toLivelloDTO(Item entity) {
        LivelloDTO dto = new LivelloDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setTipo(entity.getTipo());
        dto.setDisabled(isDisabled(entity));
        dto.setLivello(Integer.parseInt(entity.getLabel(Constants.ITEM_LIVELLO_LVL)));
        Collegamento classe = entity.getChildByType(TipoItem.CLASSE);
        Collegamento maledizione = entity.getChildByType(TipoItem.MALEDIZIONE);
        if (classe != null) {
            dto.setClasse(classe.getItemTarget().getNome());
        }
        if (maledizione != null) {
            dto.setMaledizione(maledizione.getItemTarget().getNome());
        }
        dto.setLivelliClasse(utilService.parseStringToIntList(entity.getLabel(Constants.ITEM_LIVELLO_LVL_CLASSE)));
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
}
