package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.Avanzamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AvanzamentoRepository extends JpaRepository<Avanzamento, Integer> {

    List<Avanzamento> findAllByItemSource_Id(Integer idItemSource);

    /** Somma qty espliciti per ogni itemTarget (null = non contato). Solo righe con almeno un qty != null. */
    @Query("SELECT a.itemTarget.id, SUM(a.qty) " +
           "FROM Avanzamento a " +
           "WHERE a.itemSource.id IN :sourceIds AND a.itemTarget.id IN :targetIds AND a.qty IS NOT NULL " +
           "GROUP BY a.itemTarget.id")
    List<Object[]> sumQtyByTargets(@Param("sourceIds") List<Integer> sourceIds,
                                   @Param("targetIds") List<Integer> targetIds);
}
