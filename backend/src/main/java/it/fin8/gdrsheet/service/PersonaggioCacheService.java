package it.fin8.gdrsheet.service;

import it.fin8.gdrsheet.repository.CollegamentoRepository;
import it.fin8.gdrsheet.repository.ItemLabelRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;

/**
 * Punto centrale di invalidazione per le cache (Caffeine, vedi CacheConfig) legate a un
 * personaggio. Convenzione delle chiavi: o l'id personaggio "grezzo" (Integer), o una stringa
 * che INIZIA con "<personaggioId>:" (es. per la cache items, chiavata anche per utente: vedi
 * PersonaggioService.itemsCacheKey). Ogni cache region futura che dipende da dati di un
 * personaggio deve seguire questa convenzione per poter essere invalidata da qui con una singola
 * chiamata — non serve inseguire invalidazioni fini per singolo dato.
 */
@Service
public class PersonaggioCacheService {

    private final CacheManager cacheManager;
    private final CollegamentoRepository collegamentoRepository;
    private final ItemLabelRepository itemLabelRepository;

    public PersonaggioCacheService(CacheManager cacheManager, CollegamentoRepository collegamentoRepository, ItemLabelRepository itemLabelRepository) {
        this.cacheManager = cacheManager;
        this.collegamentoRepository = collegamentoRepository;
        this.itemLabelRepository = itemLabelRepository;
    }

    /** Invalida tutte le cache region per un singolo personaggio. */
    public void invalidaPersonaggio(Integer personaggioId) {
        if (personaggioId == null) return;
        String prefix = personaggioId + ":";
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) continue;
            // chiave semplice (es. personaggioModificatori, chiavata solo per id)
            cache.evict(personaggioId);
            // chiavi composite "id:utenteId,..." (es. personaggioItems)
            if (cache instanceof CaffeineCache caffeineCache) {
                caffeineCache.getNativeCache().asMap().keySet()
                        .removeIf(key -> key instanceof String s && s.startsWith(prefix));
            }
        }
    }

    /**
     * Invalida i personaggi raggiungibili (direttamente o indirettamente, tramite il grafo
     * Collegamento) da un item toccato — necessario quando si modifica un item potenzialmente
     * condiviso tra più personaggi (es. un item di compendio linkato in più schede): la modifica
     * parte dal contesto di un personaggio, ma deve invalidare anche gli altri.
     */
    public void invalidaPerItem(Integer itemId) {
        if (itemId == null) return;
        collegamentoRepository.findPersonaggiRaggiungibiliDaItem(itemId)
                .forEach(this::invalidaPersonaggio);
    }

    /**
     * Invalida tutti i personaggi che usano una classe/razza — l'associazione LIVELLO→CLASSE
     * passa per una label (non per Collegamento), quindi non è coperta da invalidaPerItem.
     */
    public void invalidaPerClasse(Integer classeId) {
        if (classeId == null) return;
        itemLabelRepository.findPersonaggiByClasseLabel(classeId.toString())
                .forEach(this::invalidaPersonaggio);
    }
}
