package it.fin8.gdrsheet.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

/**
 * Carica le immagini degli item su un host esterno: sul database resta solo il riferimento.
 * <p>
 * Implementazione attuale: <b>Cloudinary</b>. È il terzo host tentato — Imgur ha dismesso le API
 * pubbliche, imgbb le ha messe dietro abbonamento (errore 103 su account gratuito). Cloudinary
 * offre upload <i>e</i> cancellazione via API sul piano gratuito. Il nome della classe è
 * volutamente neutro: è l'unico punto da riscrivere se si cambia host di nuovo.
 * <p>
 * Credenziali in {@code imgur.properties} (cartella di configurazione esterna, non nel repo):
 * {@code app.imghost.cloud-name}, {@code app.imghost.api-key}, {@code app.imghost.api-secret},
 * più l'opzionale {@code app.imghost.cartella}. Se mancano, la funzionalità è disattivata e gli
 * endpoint rispondono 503 con un messaggio chiaro.
 */
@Service
public class ImageHostClient {

    private static final String BASE = "https://api.cloudinary.com/v1_1/";
    /** Limite del piano gratuito di Cloudinary per singolo file. */
    private static final long MAX_BYTE = 10L * 1024 * 1024;

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private final String cartella;

    private final ObjectMapper mapper = new ObjectMapper();

    // creato alla prima richiesta e non nel costruttore: se l'host non è configurato non serve,
    // e un client HTTP costruito all'avvio è un punto di rottura in più per il boot dell'app
    private volatile HttpClient http;

    public ImageHostClient(
            @Value("${app.imghost.cloud-name:}") String cloudName,
            @Value("${app.imghost.api-key:}") String apiKey,
            @Value("${app.imghost.api-secret:}") String apiSecret,
            @Value("${app.imghost.cartella:}") String cartella
    ) {
        this.cloudName = pulisci(cloudName);
        this.apiKey = pulisci(apiKey);
        this.apiSecret = pulisci(apiSecret);
        this.cartella = pulisci(cartella);
    }

    private static String pulisci(String s) {
        return s == null ? "" : s.trim();
    }

    public boolean isConfigurato() {
        return !cloudName.isEmpty() && !apiKey.isEmpty() && !apiSecret.isEmpty();
    }

    public String nomeHost() {
        return "Cloudinary";
    }

    /** Cloudinary espone una vera API di cancellazione, quindi rimuovere elimina anche il file. */
    public boolean supportaCancellazioneRemota() {
        return true;
    }

    public long dimensioneMassimaByte() {
        return MAX_BYTE;
    }

    private HttpClient http() {
        HttpClient c = http;
        if (c == null) {
            synchronized (this) {
                if (http == null) http = HttpClient.newHttpClient();
                c = http;
            }
        }
        return c;
    }

    /**
     * Esito di un upload.
     *
     * @param url        indirizzo diretto del file (https)
     * @param riferimento identificatore del file presso l'host (public_id), serve a cancellarlo
     */
    public record Caricata(String url, String riferimento) {}

    public Caricata carica(MultipartFile file, String titolo) {
        if (!isConfigurato())
            throw new ResponseStatusException(SERVICE_UNAVAILABLE,
                    "Caricamento immagini non configurato: servono cloud-name, api-key e api-secret");
        if (file == null || file.isEmpty())
            throw new ResponseStatusException(BAD_REQUEST, "File mancante");
        if (file.getSize() > MAX_BYTE)
            throw new ResponseStatusException(BAD_REQUEST, "Immagine troppo grande (max 10 MB)");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/"))
            throw new ResponseStatusException(BAD_REQUEST, "Il file non è un'immagine");

        try {
            // parametri firmati: solo questi entrano nella firma, in ordine alfabetico
            Map<String, String> firmati = new TreeMap<>();
            firmati.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
            if (!cartella.isEmpty()) firmati.put("folder", cartella);

            StringBuilder body = new StringBuilder();
            // Cloudinary accetta il file come data URI in un normale campo di form: evita di
            // dover comporre a mano un multipart, che HttpClient non sa fare da solo
            String dataUri = "data:" + contentType + ";base64,"
                    + Base64.getEncoder().encodeToString(file.getBytes());
            aggiungi(body, "file", dataUri);
            aggiungi(body, "api_key", apiKey);
            aggiungi(body, "signature", firma(firmati));
            for (Map.Entry<String, String> e : firmati.entrySet()) aggiungi(body, e.getKey(), e.getValue());

            HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + cloudName + "/image/upload"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> resp = http().send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300)
                throw new ResponseStatusException(BAD_GATEWAY,
                        "Cloudinary ha rifiutato l'upload (HTTP " + resp.statusCode() + "): "
                                + estraiErrore(resp.body()));

            JsonNode j = mapper.readTree(resp.body());
            String link = j.path("secure_url").asText(null);
            String publicId = j.path("public_id").asText(null);
            if (link == null || link.isBlank())
                throw new ResponseStatusException(BAD_GATEWAY, "Risposta di Cloudinary senza secure_url");
            return new Caricata(link, publicId);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(BAD_GATEWAY, "Upload interrotto");
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_GATEWAY, "Errore nell'upload: " + e.getMessage());
        }
    }

    /**
     * Cancella il file dall'host. Non solleva: se fallisce, l'immagine va comunque scollegata
     * dall'item, che è ciò che conta per l'utente. Ritorna true se la cancellazione è riuscita.
     */
    public boolean cancella(String riferimento) {
        if (!isConfigurato() || riferimento == null || riferimento.isBlank()) return false;
        try {
            Map<String, String> firmati = new TreeMap<>();
            firmati.put("public_id", riferimento);
            firmati.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));

            StringBuilder body = new StringBuilder();
            aggiungi(body, "api_key", apiKey);
            aggiungi(body, "signature", firma(firmati));
            for (Map.Entry<String, String> e : firmati.entrySet()) aggiungi(body, e.getKey(), e.getValue());

            HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + cloudName + "/image/destroy"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> resp = http().send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) return false;
            return "ok".equalsIgnoreCase(mapper.readTree(resp.body()).path("result").asText(null));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Firma richiesta da Cloudinary: i parametri firmati in ordine alfabetico come
     * {@code k=v} uniti da &amp;, con l'api secret accodato, il tutto in SHA-1 esadecimale.
     * Il file, api_key e cloud_name non entrano nella firma.
     */
    private String firma(Map<String, String> firmati) throws Exception {
        StringBuilder base = new StringBuilder();
        for (Map.Entry<String, String> e : firmati.entrySet()) {
            if (base.length() > 0) base.append('&');
            base.append(e.getKey()).append('=').append(e.getValue());
        }
        base.append(apiSecret);
        byte[] hash = MessageDigest.getInstance("SHA-1")
                .digest(base.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    private static void aggiungi(StringBuilder body, String chiave, String valore) {
        if (body.length() > 0) body.append('&');
        body.append(chiave).append('=').append(URLEncoder.encode(valore, StandardCharsets.UTF_8));
    }

    /** Messaggio d'errore leggibile dalla risposta di Cloudinary, se c'è. */
    private String estraiErrore(String body) {
        try {
            String msg = mapper.readTree(body).path("error").path("message").asText(null);
            return msg != null && !msg.isBlank() ? msg : "risposta non interpretabile";
        } catch (Exception e) {
            return "risposta non interpretabile";
        }
    }
}
