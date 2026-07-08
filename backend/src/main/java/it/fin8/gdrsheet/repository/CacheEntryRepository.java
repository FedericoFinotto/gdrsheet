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

    /** Job di pulizia periodico: rimuove le righe scadute (vedi CachePuliziaJob). */
    @Modifying
    @Transactional
    @Query("DELETE FROM CacheEntry c WHERE c.dtScadenza < :now")
    int deleteExpired(@Param("now") LocalDateTime now);

    /** Invalidazione per prefisso (es. tutte le chiavi "personaggioItems::5:%" per l'utente X). */
    @Modifying
    @Transactional
    @Query("DELETE FROM CacheEntry c WHERE c.cacheKey LIKE :pattern")
    int deleteByCacheKeyLike(@Param("pattern") String pattern);
}
