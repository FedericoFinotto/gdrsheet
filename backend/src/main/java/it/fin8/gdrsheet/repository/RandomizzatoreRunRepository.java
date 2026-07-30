package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.RandomizzatoreRun;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RandomizzatoreRunRepository extends JpaRepository<RandomizzatoreRun, Integer> {

    List<RandomizzatoreRun> findByItem_IdOrderByEseguitoIlDesc(Integer idItem, Pageable pageable);

    /**
     * Potatura dello storico alla scrittura: elimina le run più vecchie oltre le ultime N di
     * quel randomizzatore. Evita di dover schedulare un job di pulizia.
     */
    @Modifying
    @Query(value = """
            DELETE FROM randomizzatore_run
            WHERE id_item = :idItem
              AND id NOT IN (
                SELECT id FROM randomizzatore_run
                WHERE id_item = :idItem
                ORDER BY eseguito_il DESC, id DESC
                LIMIT :daTenere
              )
            """, nativeQuery = true)
    int potaStorico(@Param("idItem") Integer idItem, @Param("daTenere") int daTenere);
}
