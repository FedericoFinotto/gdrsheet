package it.fin8.gdrsheet.service;

import it.fin8.gdrsheet.repository.CacheEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Pulizia periodica delle righe scadute in cache_entry. L'invalidazione "vera" è quella attiva e
 * mirata di PersonaggioCacheService: questo job è solo un backstop per non far crescere la
 * tabella con entry scadute nel frattempo (es. TTL raggiunto senza che nulla le abbia mai
 * invalidate esplicitamente).
 */
@Component
public class CachePuliziaJob {

    @Autowired
    private CacheEntryRepository cacheEntryRepository;

    @Scheduled(fixedDelay = 10 * 60 * 1000)
    public void pulisciScaduti() {
        cacheEntryRepository.deleteExpired(LocalDateTime.now());
    }
}
