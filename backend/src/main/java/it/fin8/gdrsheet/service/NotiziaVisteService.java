package it.fin8.gdrsheet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.fin8.gdrsheet.config.DbCache;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Traccia, per utente, quali notizie ha già visto (una notizia vista resta vista per sempre,
 * a differenza delle segnalazioni non serve confrontare con una data di modifica: una volta
 * apparsa e aperta, non deve più mostrare il pallino). Una riga per utente in cache_entry
 * (region "notiziaUltimaVista", chiave = username), stesso pattern di SegnalazioneVisteService.
 */
@Service
public class NotiziaVisteService {

    private final DbCache cache;

    public NotiziaVisteService(EntityManagerFactory entityManagerFactory, ObjectMapper objectMapper) {
        this.cache = new DbCache("notiziaUltimaVista", entityManagerFactory, objectMapper, Duration.ofDays(3650));
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getViste(String username) {
        Map<String, String> mappa = cache.get(username, Map.class);
        return mappa != null ? mappa : new HashMap<>();
    }

    public void segnaViste(String username, List<Integer> notiziaIds) {
        Map<String, String> mappa = getViste(username);
        String adesso = Instant.now().toString();
        for (Integer id : notiziaIds) mappa.put(id.toString(), adesso);
        cache.put(username, mappa);
    }
}
