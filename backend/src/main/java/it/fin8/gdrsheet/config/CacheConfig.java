package it.fin8.gdrsheet.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;

/**
 * Cache su tabella Postgres (cache_entry, vedi DbCache/DbCacheManager) per dati calcolati legati
 * a un personaggio (es. /items, /modificatori): nessuna infrastruttura aggiuntiva da avviare,
 * ispezionabile/cancellabile direttamente con DataGrip. L'invalidazione è attiva e mirata (vedi
 * PersonaggioCacheService), non basata su TTL: l'entryTtl qui sotto è solo un backstop di
 * sicurezza in caso di bug nell'invalidazione (pulito periodicamente da CachePuliziaJob).
 */
@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(EntityManagerFactory entityManagerFactory, ObjectMapper objectMapper) {
        // Constants.CACHE_REGIONS_PERSONAGGIO pre-registrate SUBITO (vedi DbCacheManager): serve a
        // PersonaggioCacheService.invalidaPersonaggio per poter evict-are anche una region mai
        // ancora acceduta in questa esecuzione, altrimenti una riga stale da un'esecuzione
        // precedente resterebbe servita finché non scade il TTL di backstop qui sotto.
        return new DbCacheManager(entityManagerFactory, objectMapper, Duration.ofMinutes(30),
                it.fin8.gdrsheet.config.Constants.CACHE_REGIONS_PERSONAGGIO);
    }
}
