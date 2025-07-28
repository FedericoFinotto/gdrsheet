package it.fin8.grdsheet.repository;

import it.fin8.grdsheet.entity.ItemLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemLabelRepository extends JpaRepository<ItemLabel, Integer> {
}
