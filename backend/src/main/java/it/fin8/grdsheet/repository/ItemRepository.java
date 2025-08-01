package it.fin8.grdsheet.repository;

import it.fin8.grdsheet.def.TipoItem;
import it.fin8.grdsheet.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Integer> {
    List<Item> findItemsByTipo(TipoItem tipo);

    Item findItemById(Integer id);
}
