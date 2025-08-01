package it.fin8.grdsheet.repository;

import it.fin8.grdsheet.entity.StatValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatValueRepository extends JpaRepository<StatValue, Integer> {
    List<StatValue> findAllByPersonaggioId(Integer id);

    @Query("SELECT sv FROM StatValue sv " +
            " JOIN FETCH sv.stat s" +
            " LEFT JOIN FETCH sv.mod m" +
            " WHERE sv.personaggio.id = :id")
    List<StatValue> findAllByPersonaggioIdWithStat(@Param("id") Integer id);


}
