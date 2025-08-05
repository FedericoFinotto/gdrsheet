package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.Modificatore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ModificatoreRepository extends JpaRepository<Modificatore, Integer> {
    List<Modificatore> findAllByItemIdIn(Collection<Integer> itemIds);
}
