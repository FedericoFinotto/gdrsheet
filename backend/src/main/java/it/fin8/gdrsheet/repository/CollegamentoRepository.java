package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.Collegamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CollegamentoRepository extends JpaRepository<Collegamento, Integer> {

    List<Collegamento> findAllByItemSource_Id(Integer id);

    List<Collegamento> findAllByItemTarget_Id(Integer id);

    /** Somma qty espliciti per ogni itemTarget (null = non contato). Solo righe con almeno un qty != null. */
    @Query("SELECT c.itemTarget.id, SUM(c.qty) " +
           "FROM Collegamento c " +
           "WHERE c.itemSource.id IN :sourceIds AND c.itemTarget.id IN :targetIds AND c.qty IS NOT NULL " +
           "GROUP BY c.itemTarget.id")
    List<Object[]> sumQtyByTargets(@Param("sourceIds") List<Integer> sourceIds,
                                   @Param("targetIds") List<Integer> targetIds);
}
