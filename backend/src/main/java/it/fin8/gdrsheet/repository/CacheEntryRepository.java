package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.CacheEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
public interface CacheEntryRepository extends JpaRepository<CacheEntry, String> {

    /** Job di pulizia periodico: rimuove le righe scadute (vedi CachePuliziaJob). Gira su un
     * thread schedulato senza sessione OSIV, quindi qui il repository condiviso è sicuro. */
    @Modifying
    @Transactional
    @Query("DELETE FROM CacheEntry c WHERE c.dtScadenza < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
