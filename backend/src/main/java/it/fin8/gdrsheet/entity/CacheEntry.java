package it.fin8.gdrsheet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Riga di cache generica chiave/valore (JSON), con scadenza esplicita. Vedi
 * it.fin8.gdrsheet.config.DbCacheManager per il backend Cache di Spring che la usa, e
 * PersonaggioCacheService per l'invalidazione mirata.
 */
@Getter
@Setter
@Entity
@Table(name = "cache_entry")
public class CacheEntry {

    @Id
    @Column(name = "cache_key", nullable = false, length = 255)
    private String cacheKey;

    @Column(name = "valore", nullable = false, columnDefinition = "text")
    private String valore;

    @Column(name = "dt_scadenza", nullable = false)
    private LocalDateTime dtScadenza;
}
