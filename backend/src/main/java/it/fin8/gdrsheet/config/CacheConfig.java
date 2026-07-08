package it.fin8.gdrsheet.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache in-memory (Caffeine, nessuna infrastruttura esterna) per dati calcolati legati a un
 * personaggio (es. /items, formule attacchi). L'invalidazione è attiva e mirata (vedi
 * PersonaggioCacheService), non basata su TTL: l'expireAfterWrite qui sotto è solo un backstop
 * di sicurezza in caso di bug nell'invalidazione, non il meccanismo primario.
 * <p>
 * CaffeineCacheManager senza setCacheNames(...) crea le cache region "al volo" alla prima
 * richiesta di un nome non ancora visto: non serve elencare qui i nomi delle cache che verranno
 * aggiunte via @Cacheable in futuro.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(10_000));
        return manager;
    }
}
