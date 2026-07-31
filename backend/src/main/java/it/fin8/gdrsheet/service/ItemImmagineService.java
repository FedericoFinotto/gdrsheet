package it.fin8.gdrsheet.service;

import it.fin8.gdrsheet.dto.ItemImmagineDTO;
import it.fin8.gdrsheet.entity.Item;
import it.fin8.gdrsheet.entity.ItemImmagine;
import it.fin8.gdrsheet.repository.ItemImmagineRepository;
import it.fin8.gdrsheet.repository.ItemRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Immagini di un item: il file viene caricato su un host esterno (vedi {@link ImageHostClient}),
 * in locale resta solo il riferimento.
 */
@Service
public class ItemImmagineService {

    private final ItemRepository itemRepository;
    private final ItemImmagineRepository immagineRepository;
    private final ImageHostClient host;

    public ItemImmagineService(ItemRepository itemRepository,
                               ItemImmagineRepository immagineRepository,
                               ImageHostClient host) {
        this.itemRepository = itemRepository;
        this.immagineRepository = immagineRepository;
        this.host = host;
    }

    public List<ItemImmagineDTO> getImmagini(Integer idItem) {
        return immagineRepository.findByItem_IdOrderByOrdineAscIdAsc(idItem).stream()
                .map(i -> new ItemImmagineDTO(i.getId(), i.getUrl(), i.getTitolo(), i.getOrdine()))
                .toList();
    }

    @Transactional
    public ItemImmagineDTO carica(Integer idItem, MultipartFile file, String titolo) {
        Item item = itemRepository.findItemById(idItem);
        if (item == null) throw new ResponseStatusException(NOT_FOUND, "Item non trovato");

        // l'upload avviene prima di scrivere: se l'host rifiuta non resta una riga orfana
        ImageHostClient.Caricata caricata = host.carica(file, titolo);

        ItemImmagine img = new ItemImmagine();
        img.setItem(item);
        img.setUrl(caricata.url());
        img.setRiferimentoEsterno(caricata.riferimento());
        img.setTitolo(titolo != null && !titolo.isBlank() ? titolo.trim() : null);
        img.setOrdine(immagineRepository.ordineMassimo(idItem) + 1);
        img.setCaricataIl(LocalDateTime.now());
        ItemImmagine salvata = immagineRepository.save(img);

        return new ItemImmagineDTO(salvata.getId(), salvata.getUrl(), salvata.getTitolo(),
                salvata.getOrdine());
    }

    /**
     * Scollega l'immagine dall'item e cancella il file dall'host.
     * <p>
     * Se la cancellazione remota fallisce si procede comunque: per l'utente conta che
     * l'immagine sparisca dall'item, e un file rimasto sull'host non è un errore da mostrargli.
     */
    @Transactional
    public void elimina(Integer idImmagine) {
        ItemImmagine img = immagineRepository.findById(idImmagine)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Immagine non trovata"));
        host.cancella(img.getRiferimentoEsterno());
        immagineRepository.delete(img);
    }

    /** Riordina le immagini di un item secondo la sequenza di id ricevuta. */
    @Transactional
    public List<ItemImmagineDTO> riordina(Integer idItem, List<Integer> idInOrdine) {
        List<ItemImmagine> attuali = immagineRepository.findByItem_IdOrderByOrdineAscIdAsc(idItem);
        for (ItemImmagine img : attuali) {
            int pos = idInOrdine.indexOf(img.getId());
            img.setOrdine(pos >= 0 ? pos : Integer.MAX_VALUE);
        }
        immagineRepository.saveAll(attuali);
        return getImmagini(idItem);
    }
}
