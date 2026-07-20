package it.fin8.gdrsheet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.fin8.gdrsheet.config.DbCache;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Traccia, per utente, l'ultima volta che ha visto ogni segnalazione (per evidenziare commenti
 * e variazioni non lette in Segnalazioni.vue). Una riga per utente in cache_entry (region
 * "segUltimaVista", chiave = username): questo dato non è un cache ricalcolabile, quindi usa
 * un TTL volutamente molto lungo invece di quello di CacheConfig (30 min, pensato per dati
 * derivati), dato che DbCache non supporta l'assenza di scadenza.
 */
@Service
public class SegnalazioneVisteService {

    private final DbCache cache;

    public SegnalazioneVisteService(EntityManagerFactory entityManagerFactory, ObjectMapper objectMapper) {
        this.cache = new DbCache("segUltimaVista", entityManagerFactory, objectMapper, Duration.ofDays(3650));
    }

    /** Mappa id segnalazione (come stringa) -> timestamp ISO-8601 dell'ultima visita. */
    @SuppressWarnings("unchecked")
    public Map<String, String> getViste(String username) {
        Map<String, String> mappa = cache.get(username, Map.class);
        return mappa != null ? mappa : new HashMap<>();
    }

    public void segnaVista(String username, Integer segnalazioneId) {
        Map<String, String> mappa = getViste(username);
        mappa.put(segnalazioneId.toString(), Instant.now().toString());
        cache.put(username, mappa);
    }
}
