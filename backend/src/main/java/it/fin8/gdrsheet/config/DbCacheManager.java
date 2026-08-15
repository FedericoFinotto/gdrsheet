package it.fin8.gdrsheet.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * CacheManager che crea le cache region "al volo" alla prima richiesta di un nome non ancora
 * visto (come farebbe CaffeineCacheManager/RedisCacheManager in modalità dinamica): non serve
 * elencare qui i nomi delle cache che verranno aggiunte in futuro con altri @Cacheable/get-put.
 * <p>
 * ECCEZIONE: {@code knownRegionNames} (vedi costruttore) va pre-registrato SUBITO, non alla prima
 * richiesta — {@link #getCacheNames()} è usato da PersonaggioCacheService.invalidaPersonaggio per
 * decidere QUALI region evict-are, e se una region non è ancora "conosciuta" in questa esecuzione
 * della JVM (perché nessuno l'ha ancora richiesta con getCache) l'eviction la salta del tutto —
 * lasciando servita per il resto del TTL una riga STALE eventualmente rimasta nella tabella
 * cache_entry da un'esecuzione precedente (il bug concreto: si modifica una classe/razza subito
 * dopo un riavvio, PRIMA di aver mai aperto la scheda di un personaggio in questa esecuzione —
 * l'invalidazione gira a vuoto, poi la prima GET /items trova ed espone il valore vecchio).
 */
public class DbCacheManager implements CacheManager {

    private final EntityManagerFactory entityManagerFactory;
    private final ObjectMapper objectMapper;
    private final Duration ttl;
    private final ConcurrentMap<String, DbCache> caches = new ConcurrentHashMap<>();

    public DbCacheManager(EntityManagerFactory entityManagerFactory, ObjectMapper objectMapper, Duration ttl) {
        this(entityManagerFactory, objectMapper, ttl, Set.of());
    }

    public DbCacheManager(EntityManagerFactory entityManagerFactory, ObjectMapper objectMapper, Duration ttl,
                           Set<String> knownRegionNames) {
        this.entityManagerFactory = entityManagerFactory;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
        knownRegionNames.forEach(this::getCache);
    }

    @Override
    public Cache getCache(String name) {
        return caches.computeIfAbsent(name, n -> new DbCache(n, entityManagerFactory, objectMapper, ttl));
    }

    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(caches.keySet());
    }
}
