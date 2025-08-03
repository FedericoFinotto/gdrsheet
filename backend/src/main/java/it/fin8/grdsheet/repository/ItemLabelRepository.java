package it.fin8.grdsheet.repository;

import it.fin8.grdsheet.entity.ItemLabel;
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
}
