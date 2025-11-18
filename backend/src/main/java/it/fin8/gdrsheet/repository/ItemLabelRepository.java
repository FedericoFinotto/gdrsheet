package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.ItemLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemLabelRepository extends JpaRepository<ItemLabel, Integer> {
    public List<ItemLabel> findByItemPersonaggioIdAndItemNome(
            Integer personaggioId,
            String itemName
    );

    /**
     * Restituisce tutti gli ItemLabel che hanno esattamente quella label
     * e il cui item.id è contenuto nella lista itemIds.
     */
    List<ItemLabel> findByLabelAndItem_IdIn(String label, List<Integer> itemIds);

    List<ItemLabel> findByLabelLikeAndItem_IdIn(String label, List<Integer> itemIds);
}
