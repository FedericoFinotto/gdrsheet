package it.fin8.gdrsheet.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import it.fin8.gdrsheet.entity.Utente;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtService {

    private final SecretKey key;
    private final long validityMillis;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.validity-hours:168}") long validityHours
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validityMillis = validityHours * 60 * 60 * 1000;
    }

    public String generateToken(Utente utente) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(utente.getId()))
                .claim("username", utente.getUsername())
                .claim("ruolo", utente.getRuolo())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + validityMillis))
                .signWith(key)
                .compact();
    }

    /**
     * Valida il token e ritorna l'id utente (subject), oppure null se non valido/scaduto.
     */
    public Integer validateAndGetUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Integer.parseInt(claims.getSubject());
        } catch (JwtException | NumberFormatException e) {
            return null;
        }
    }
}
