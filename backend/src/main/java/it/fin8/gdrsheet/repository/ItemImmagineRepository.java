package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.ItemImmagine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ItemImmagineRepository extends JpaRepository<ItemImmagine, Integer> {

    List<ItemImmagine> findByItem_IdOrderByOrdineAscIdAsc(Integer idItem);

    List<ItemImmagine> findByItem_IdInOrderByOrdineAscIdAsc(Collection<Integer> idItems);

    @Query("SELECT coalesce(max(i.ordine), -1) FROM ItemImmagine i WHERE i.item.id = :idItem")
    int ordineMassimo(@Param("idItem") Integer idItem);
}
