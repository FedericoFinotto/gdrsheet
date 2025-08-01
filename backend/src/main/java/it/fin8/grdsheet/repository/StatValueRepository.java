package it.fin8.grdsheet.repository;

import it.fin8.grdsheet.entity.StatValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatValueRepository extends JpaRepository<StatValue, Integer> {
    List<StatValue> findAllByPersonaggioId(Integer id);

}
