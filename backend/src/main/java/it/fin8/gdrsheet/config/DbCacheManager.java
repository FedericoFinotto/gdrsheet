package it.fin8.gdrsheet.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * CacheManager che crea le cache region "al volo" alla prima richiesta di un nome non ancora
 * visto (come farebbe CaffeineCacheManager/RedisCacheManager in modalità dinamica): non serve
 * elencare qui i nomi delle cache che verranno aggiunte in futuro con altri @Cacheable/get-put.
 */
public class DbCacheManager implements CacheManager {

    private final EntityManagerFactory entityManagerFactory;
    private final ObjectMapper objectMapper;
    private final Duration ttl;
    private final ConcurrentMap<String, DbCache> caches = new ConcurrentHashMap<>();

    public DbCacheManager(EntityManagerFactory entityManagerFactory, ObjectMapper objectMapper, Duration ttl) {
        this.entityManagerFactory = entityManagerFactory;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
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
