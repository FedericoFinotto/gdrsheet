package it.fin8.gdrsheet.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.fin8.gdrsheet.entity.CacheEntry;
import it.fin8.gdrsheet.repository.CacheEntryRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;

/**
 * Cache region Spring backata da una riga per chiave nella tabella cache_entry (vedi
 * DbCacheManager e CacheConfig). Ogni entry è JSON serializzato con l'ObjectMapper dell'app,
 * cosi' è leggibile/cancellabile a mano da DataGrip.
 */
public class DbCache implements Cache {

    private final String name;
    private final CacheEntryRepository repository;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public DbCache(String name, CacheEntryRepository repository, ObjectMapper objectMapper, Duration ttl) {
        this.name = name;
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return repository;
    }

    private String fullKey(Object key) {
        return name + "::" + key;
    }

    private CacheEntry loadValid(Object key) {
        CacheEntry entry = repository.findById(fullKey(key)).orElse(null);
        if (entry == null) return null;
        if (entry.getDtScadenza().isBefore(LocalDateTime.now())) {
            repository.deleteById(entry.getCacheKey());
            return null;
        }
        return entry;
    }

    @Override
    public ValueWrapper get(Object key) {
        CacheEntry entry = loadValid(key);
        if (entry == null) return null;
        try {
            return new SimpleValueWrapper(objectMapper.readValue(entry.getValore(), Object.class));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        CacheEntry entry = loadValid(key);
        if (entry == null) return null;
        try {
            return objectMapper.readValue(entry.getValore(), type);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper wrapper = get(key);
        if (wrapper != null) return (T) wrapper.get();
        try {
            T value = valueLoader.call();
            put(key, value);
            return value;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(Object key, Object value) {
        if (value == null) {
            evict(key);
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            CacheEntry entry = repository.findById(fullKey(key)).orElseGet(CacheEntry::new);
            entry.setCacheKey(fullKey(key));
            entry.setValore(json);
            entry.setDtScadenza(LocalDateTime.now().plus(ttl));
            repository.save(entry);
        } catch (Exception ignored) {
            // valore non serializzabile: nessuna cache, si ricalcola al prossimo giro
        }
    }

    @Override
    public void evict(Object key) {
        repository.deleteById(fullKey(key));
    }

    @Override
    public void clear() {
        repository.deleteByCacheKeyLike(name + "::%");
    }
}
