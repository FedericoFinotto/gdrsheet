package it.fin8.gdrsheet.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.fin8.gdrsheet.repository.CacheEntryRepository;
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
    public CacheManager cacheManager(CacheEntryRepository cacheEntryRepository, ObjectMapper objectMapper) {
        return new DbCacheManager(cacheEntryRepository, objectMapper, Duration.ofMinutes(30));
    }
}
