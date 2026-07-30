package it.fin8.gdrsheet.service;

import it.fin8.gdrsheet.config.Constants;
import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.dto.ItemTagDTO;
import it.fin8.gdrsheet.entity.Item;
import it.fin8.gdrsheet.entity.ItemTag;
import it.fin8.gdrsheet.repository.ItemRepository;
import it.fin8.gdrsheet.repository.ItemTagRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Gestione dei tag pesati associati a un oggetto (tabella item_tag).
 * <p>
 * La categoria non viene mai presa dal client: è sempre derivata dal tag (label CATEGORIA)
 * e salvata denormalizzata, così la selezione del randomizzatore resta risolvibile con un
 * solo scan di item_tag.
 */
@Service
public class ItemTagService {

    private final ItemRepository itemRepository;
    private final ItemTagRepository itemTagRepository;

    public ItemTagService(ItemRepository itemRepository, ItemTagRepository itemTagRepository) {
        this.itemRepository = itemRepository;
        this.itemTagRepository = itemTagRepository;
    }

    public List<ItemTagDTO> getTags(Integer idItem) {
        return itemTagRepository.findByItemIdFetch(idItem).stream()
                .map(t -> new ItemTagDTO(
                        t.getTag().getId(), t.getTag().getNome(),
                        t.getCategoria().getId(), t.getCategoria().getNome(),
                        t.getPeso()))
                .toList();
    }

    /**
     * Sostituisce integralmente i tag dell'oggetto (stessa semantica delle labels).
     * Righe con peso assente o &lt;= 0 vengono scartate: "peso 0" e "tag assente" coincidono.
     */
    @Transactional
    public List<ItemTagDTO> setTags(Integer idItem, List<ItemTagDTO> richiesti) {
        Item item = itemRepository.findItemById(idItem);
        if (item == null) throw new ResponseStatusException(NOT_FOUND, "Item non trovato");

        itemTagRepository.deleteByItem_Id(idItem);
        itemTagRepository.flush();

        if (richiesti == null || richiesti.isEmpty()) return List.of();

        Set<Integer> giaVisti = new HashSet<>();
        List<ItemTag> daSalvare = new ArrayList<>();
        for (ItemTagDTO riga : richiesti) {
            if (riga.getIdTag() == null) continue;
            if (riga.getPeso() == null || riga.getPeso().compareTo(BigDecimal.ZERO) <= 0) continue;
            if (!giaVisti.add(riga.getIdTag())) continue;   // stesso tag due volte: la prima vince

            Item tag = itemRepository.findItemById(riga.getIdTag());
            if (tag == null || !TipoItem.TAG.equals(tag.getTipo()))
                throw new ResponseStatusException(BAD_REQUEST, "Tag non valido: " + riga.getIdTag());

            Item categoria = categoriaDelTag(tag);
            ItemTag nuovo = new ItemTag();
            nuovo.setItem(item);
            nuovo.setTag(tag);
            nuovo.setCategoria(categoria);
            nuovo.setPeso(riga.getPeso());
            daSalvare.add(nuovo);
        }
        itemTagRepository.saveAll(daSalvare);
        return getTags(idItem);
    }

    /** Categoria di appartenenza di un tag, dalla sua label CATEGORIA. */
    private Item categoriaDelTag(Item tag) {
        String raw = tag.getLabel(Constants.ITEM_LABEL_TAG_CATEGORIA);
        Item categoria = null;
        if (raw != null && !raw.isBlank()) {
            try {
                categoria = itemRepository.findItemById(Integer.valueOf(raw.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (categoria == null || !TipoItem.CATEGORIA.equals(categoria.getTipo()))
            throw new ResponseStatusException(BAD_REQUEST,
                    "Il tag '" + tag.getNome() + "' non è associato a nessuna categoria");
        return categoria;
    }

    /**
     * Da richiamare quando un TAG cambia categoria: riallinea la colonna denormalizzata su
     * tutte le associazioni esistenti. Senza questo, i vecchi item_tag continuerebbero a
     * riferire la categoria precedente.
     */
    @Transactional
    public void riallineaCategoria(Item tag) {
        if (!TipoItem.TAG.equals(tag.getTipo())) return;
        String raw = tag.getLabel(Constants.ITEM_LABEL_TAG_CATEGORIA);
        if (raw == null || raw.isBlank()) return;
        try {
            itemTagRepository.aggiornaCategoriaDenormalizzata(tag.getId(), Integer.valueOf(raw.trim()));
        } catch (NumberFormatException ignored) {
        }
    }
}
