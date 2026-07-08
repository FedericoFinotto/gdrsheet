package it.fin8.gdrsheet.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.fin8.gdrsheet.repository.CacheEntryRepository;
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

    private final CacheEntryRepository repository;
    private final ObjectMapper objectMapper;
    private final Duration ttl;
    private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<>();

    public DbCacheManager(CacheEntryRepository repository, ObjectMapper objectMapper, Duration ttl) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    @Override
    public Cache getCache(String name) {
        return caches.computeIfAbsent(name, n -> new DbCache(n, repository, objectMapper, ttl));
    }

    @Override
    public Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(caches.keySet());
    }
}
