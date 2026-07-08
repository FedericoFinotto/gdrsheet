package it.fin8.gdrsheet.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.fin8.gdrsheet.entity.CacheEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;

/**
 * Cache region Spring backata da una riga per chiave nella tabella cache_entry (vedi
 * DbCacheManager e CacheConfig). Ogni entry è JSON serializzato con l'ObjectMapper dell'app,
 * cosi' è leggibile/cancellabile a mano da DataGrip.
 * <p>
 * IMPORTANTE: ogni operazione usa un EntityManager DEDICATO e isolato (creato al volo dalla
 * EntityManagerFactory, con la sua transazione), MAI quello condiviso della richiesta (OSIV).
 * Un normale JpaRepository qui condividerebbe la Session della richiesta corrente: il suo commit
 * farebbe un flush dell'INTERA sessione, persistendo anche le mutazioni transitorie che
 * flattenItems() lascia sugli Item (label DISABILITATO impostata solo per il rendering) — lo
 * stesso identico bug già risolto altrove per PESO_EFFETTIVO (vedi PersonaggioService.cachePesoEffettivo).
 */
public class DbCache implements Cache {

    private static final Logger log = LoggerFactory.getLogger(DbCache.class);

    private final String name;
    private final EntityManagerFactory entityManagerFactory;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public DbCache(String name, EntityManagerFactory entityManagerFactory, ObjectMapper objectMapper, Duration ttl) {
        this.name = name;
        this.entityManagerFactory = entityManagerFactory;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return entityManagerFactory;
    }

    private String fullKey(Object key) {
        return name + "::" + key;
    }

    private CacheEntry loadValid(Object key) {
        String fk = fullKey(key);
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            CacheEntry entry = em.find(CacheEntry.class, fk);
            if (entry == null) return null;
            if (entry.getDtScadenza().isBefore(LocalDateTime.now())) {
                deleteKey(fk);
                return null;
            }
            return entry;
        } finally {
            em.close();
        }
    }

    private void deleteKey(String fullKey) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            CacheEntry ref = em.find(CacheEntry.class, fullKey);
            if (ref != null) em.remove(ref);
            tx.commit();
        } catch (Exception e) {
            log.warn("Cache '{}': errore evict chiave '{}'", name, fullKey, e);
            if (tx.isActive()) {
                try { tx.rollback(); } catch (Exception ignored) {}
            }
        } finally {
            em.close();
        }
    }

    /** Cancella tutte le chiavi di questa region il cui suffisso (dopo "name::") inizia con {@code prefix}. */
    public void evictByPrefix(String prefix) {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("DELETE FROM CacheEntry c WHERE c.cacheKey LIKE :pattern")
                    .setParameter("pattern", fullKey(prefix) + "%")
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            log.warn("Cache '{}': errore evictByPrefix '{}'", name, prefix, e);
            if (tx.isActive()) {
                try { tx.rollback(); } catch (Exception ignored) {}
            }
        } finally {
            em.close();
        }
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
        String json;
        try {
            json = objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Cache '{}': valore non serializzabile per chiave '{}', non cachato", name, key, e);
            return; // valore non serializzabile: nessuna cache, si ricalcola al prossimo giro
        }

        String fk = fullKey(key);
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            CacheEntry entry = em.find(CacheEntry.class, fk);
            boolean nuova = entry == null;
            if (nuova) {
                entry = new CacheEntry();
                entry.setCacheKey(fk);
            }
            entry.setValore(json);
            entry.setDtScadenza(LocalDateTime.now().plus(ttl));
            if (nuova) em.persist(entry);
            tx.commit();
        } catch (Exception e) {
            log.warn("Cache '{}': errore put chiave '{}'", name, fk, e);
            if (tx.isActive()) {
                try { tx.rollback(); } catch (Exception ignored) {}
            }
        } finally {
            em.close();
        }
    }

    @Override
    public void evict(Object key) {
        deleteKey(fullKey(key));
    }

    @Override
    public void clear() {
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("DELETE FROM CacheEntry c WHERE c.cacheKey LIKE :pattern")
                    .setParameter("pattern", name + "::%")
                    .executeUpdate();
            tx.commit();
        } catch (Exception e) {
            log.warn("Cache '{}': errore clear", name, e);
            if (tx.isActive()) {
                try { tx.rollback(); } catch (Exception ignored) {}
            }
        } finally {
            em.close();
        }
    }
}
