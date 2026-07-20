package it.fin8.gdrsheet.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

/**
 * Client minimale verso l'API REST di Taiga (self-hosted), usato per la feature "Segnalazioni".
 * Autentica con l'account bot dedicato e cachea in memoria token/id progetto/stato iniziale,
 * dato che non cambiano durante la vita del processo.
 * <p>
 * Le credenziali vengono lette da un file esterno nella stessa cartella di configurazione
 * usata per il database ({@code DatabaseConfig}), non dal repo: se il file non esiste
 * (es. in dev) si ricade sui default vuoti definiti in application-local.properties.
 */
@Service
public class TaigaClient {

    private final String baseUrl;
    private final String projectSlug;
    private final String botUsername;
    private final String botPassword;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private volatile String authToken;
    private volatile Integer projectId;
    private volatile Integer newStatusId;

    public TaigaClient(
            @Value("${app.taiga.base-url:}") String baseUrl,
            @Value("${app.taiga.project-slug:}") String projectSlug,
            @Value("${app.taiga.bot-username:}") String botUsername,
            @Value("${app.taiga.bot-password:}") String botPassword
    ) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        this.projectSlug = projectSlug;
        this.botUsername = botUsername;
        this.botPassword = botPassword;
    }

    public record Segnalazione(Integer id, Integer ref, String titolo, String descrizione, String stato,
                               String dataCreazione, String dataModifica, List<String> tags) {
    }
    public record Commento(String autore, String testo, String data) {}

    public record Allegato(Integer id, String nome, String url) {
    }

    private static List<String> parseTags(JsonNode tagsNode) {
        List<String> out = new ArrayList<>();
        for (JsonNode t : tagsNode) {
            if (t.isTextual()) out.add(t.asText());
            else if (t.isArray() && t.size() > 0) out.add(t.get(0).asText());
        }
        return out;
    }

    public List<Segnalazione> listUserStoriesByTag(String tag) throws IOException, InterruptedException {
        int project = getProjectId();
        String url = baseUrl + "/api/v1/userstories?project=" + project + "&tags=" + urlEncode(tag);
        return parseUserStories(requestJson("GET", url, null, true));
    }

    /** Tutte le user story del progetto (non solo quelle etichettate con l'utente), sempre escludendo gli archiviati. */
    public List<Segnalazione> listAllUserStories() throws IOException, InterruptedException {
        int project = getProjectId();
        String url = baseUrl + "/api/v1/userstories?project=" + project;
        return parseUserStories(requestJson("GET", url, null, true));
    }

    private List<Segnalazione> parseUserStories(JsonNode arr) {
        List<Segnalazione> out = new ArrayList<>();
        for (JsonNode n : arr) {
            // Solo lo stato "Archiviato/Archived" va escluso sempre: gli altri stati chiusi
            // (In corso, Concluso, Deployato, ...) restano visibili, is_closed non basta a distinguerli.
            String stato = n.path("status_extra_info").path("name").asText("");
            if (stato.toLowerCase().contains("archivi") || stato.toLowerCase().contains("archive")) continue;
            out.add(new Segnalazione(
                    n.path("id").asInt(),
                    n.path("ref").asInt(),
                    n.path("subject").asText(null),
                    n.path("description").asText(null),
                    n.path("status_extra_info").path("name").asText(null),
                    n.path("created_date").asText(null),
                    n.path("modified_date").asText(null),
                    parseTags(n.path("tags"))
            ));
        }
        return out;
    }

    /** La lista di Taiga (userstories?project=...) omette la descrizione: serve il dettaglio per averla. */
    public Segnalazione getUserStory(Integer id) throws IOException, InterruptedException {
        JsonNode n = requestJson("GET", baseUrl + "/api/v1/userstories/" + id, null, true);
        return new Segnalazione(
                n.path("id").asInt(),
                n.path("ref").asInt(),
                n.path("subject").asText(null),
                n.path("description").asText(null),
                n.path("status_extra_info").path("name").asText(null),
                n.path("created_date").asText(null),
                n.path("modified_date").asText(null),
                parseTags(n.path("tags"))
        );
    }

    /**
     * Modifica titolo/descrizione: il chiamante deve aver già verificato che l'utente sia il proprietario.
     */
    public Segnalazione updateUserStory(Integer id, String titolo, String descrizione) throws IOException, InterruptedException {
        JsonNode current = requestJson("GET", baseUrl + "/api/v1/userstories/" + id, null, true);
        int version = current.path("version").asInt();

        ObjectNode body = mapper.createObjectNode();
        body.put("version", version);
        body.put("subject", titolo);
        body.put("description", descrizione == null ? "" : descrizione);

        JsonNode updated = requestJson("PATCH", baseUrl + "/api/v1/userstories/" + id, body.toString(), true);
        return new Segnalazione(
                updated.path("id").asInt(),
                updated.path("ref").asInt(),
                updated.path("subject").asText(null),
                updated.path("description").asText(null),
                updated.path("status_extra_info").path("name").asText(null),
                updated.path("created_date").asText(null),
                updated.path("modified_date").asText(null),
                parseTags(updated.path("tags"))
        );
    }

    private static final java.util.Set<String> ESTENSIONI_IMMAGINE =
            java.util.Set.of("png", "jpg", "jpeg", "gif", "webp", "bmp");

    private static boolean isImage(String nome) {
        if (nome == null) return false;
        int punto = nome.lastIndexOf('.');
        if (punto < 0) return false;
        return ESTENSIONI_IMMAGINE.contains(nome.substring(punto + 1).toLowerCase());
    }

    /**
     * Solo le immagini: è l'unico tipo di allegato previsto e mostrato dall'app.
     */
    public List<Allegato> getAttachments(Integer userStoryId) throws IOException, InterruptedException {
        int project = getProjectId();
        String url = baseUrl + "/api/v1/userstories/attachments?project=" + project + "&object_id=" + userStoryId;
        JsonNode arr = requestJson("GET", url, null, true);
        List<Allegato> out = new ArrayList<>();
        for (JsonNode n : arr) {
            String nome = n.path("name").asText(null);
            if (!isImage(nome)) continue;
            // "url" è già assoluto e firmato con un token temporaneo richiesto da taiga-protected
            // per servire l'allegato: "attached_file" è solo il path grezzo, senza firma, e da
            // solo restituisce 403 (il controllo non si basa sull'header Authorization).
            String fileUrl = n.path("url").asText(null);
            if (fileUrl == null || fileUrl.isBlank()) fileUrl = assolutizza(n.path("attached_file").asText(null));
            out.add(new Allegato(n.path("id").asInt(), nome, fileUrl));
        }
        return out;
    }

    /**
     * Taiga a volte restituisce "attached_file" come path relativo invece di URL assoluto,
     * e su questa installazione manca anche il prefisso "/media/" richiesto da nginx per
     * instradare la richiesta al servizio protetto degli allegati (MEDIA_URL mal configurato
     * lato Taiga): senza quel prefisso la richiesta cade nel catch-all della SPA.
     */
    private String assolutizza(String url) {
        if (url == null || url.isBlank()) return null;
        String assoluto = (url.startsWith("http://") || url.startsWith("https://"))
                ? url
                : baseUrl + (url.startsWith("/") ? url : "/" + url);
        if (assoluto.contains("/attachments/") && !assoluto.contains("/media/attachments/")) {
            assoluto = assoluto.replace("/attachments/", "/media/attachments/");
        }
        return assoluto;
    }

    public record Contenuto(byte[] bytes, String contentType) {
    }

    /**
     * Scarica i byte di un allegato autenticandosi con il token del bot (l'URL da solo non basta se Taiga richiede auth).
     */
    public Contenuto scaricaAllegato(String fileUrl) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fileUrl))
                .header("Authorization", "Bearer " + getAuthToken(false))
                .GET()
                .build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == 401) {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(fileUrl))
                    .header("Authorization", "Bearer " + getAuthToken(true))
                    .GET()
                    .build();
            response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        }
        if (response.statusCode() >= 300) {
            throw new IOException("Download allegato fallito: HTTP " + response.statusCode());
        }
        String contentType = response.headers().firstValue("Content-Type").orElse("application/octet-stream");
        return new Contenuto(response.body(), contentType);
    }

    public Segnalazione createUserStory(String titolo, String descrizione, String tag) throws IOException, InterruptedException {
        int project = getProjectId();
        int status = getNewStatusId(project);

        ObjectNode body = mapper.createObjectNode();
        body.put("project", project);
        body.put("subject", titolo);
        body.put("description", descrizione == null ? "" : descrizione);
        body.put("status", status);
        body.putArray("tags").add(tag);

        JsonNode created = requestJson("POST", baseUrl + "/api/v1/userstories", body.toString(), true);
        return new Segnalazione(
                created.path("id").asInt(),
                created.path("ref").asInt(),
                created.path("subject").asText(null),
                created.path("description").asText(null),
                created.path("status_extra_info").path("name").asText(null),
                created.path("created_date").asText(null),
                created.path("modified_date").asText(null),
                parseTags(created.path("tags"))
        );
    }

    /** Best-effort: se l'upload fallisce non deve bloccare la creazione della segnalazione. */
    public void uploadAttachment(Integer userStoryId, MultipartFile file) {
        try {
            int project = getProjectId();
            String boundary = "----taiga-" + UUID.randomUUID();
            byte[] fileBytes = file.getBytes();
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "screenshot.png";
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

            var out = new java.io.ByteArrayOutputStream();
            String header =
                    "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"project\"\r\n\r\n" + project + "\r\n" +
                    "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"object_id\"\r\n\r\n" + userStoryId + "\r\n" +
                    "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"attached_file\"; filename=\"" + filename + "\"\r\n" +
                    "Content-Type: " + contentType + "\r\n\r\n";
            out.write(header.getBytes(StandardCharsets.UTF_8));
            out.write(fileBytes);
            out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/v1/userstories/attachments"))
                    .header("Authorization", "Bearer " + getAuthToken(false))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()))
                    .build();
            http.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
            // best-effort: lo screenshot non è mai bloccante per la creazione della segnalazione
        }
    }

    // Il bot Taiga è l'unico autore possibile lato API: per far vedere l'utente reale
    // (sia in app che su Taiga stesso) si prefissa il testo con "USERNAME: " e lo si
    // riestrae in lettura, così i commenti scritti dall'app mostrano l'autore vero
    // anche se tecnicamente sono tutti pubblicati dal bot.
    private static final java.util.regex.Pattern AUTORE_PREFIX = java.util.regex.Pattern.compile("^([A-Z0-9_.\\-]+): ([\\s\\S]*)$");

    public List<Commento> getComments(Integer userStoryId) throws IOException, InterruptedException {
        String url = baseUrl + "/api/v1/history/userstory/" + userStoryId;
        JsonNode arr = requestJson("GET", url, null, true);
        List<Commento> out = new ArrayList<>();
        for (JsonNode n : arr) {
            if (!n.path("delete_comment_date").isNull()) continue;
            String testo = n.path("comment").asText(null);
            if (testo == null || testo.isBlank()) continue;

            String autore = n.path("user").path("name").asText(n.path("user").path("username").asText(null));
            var matcher = AUTORE_PREFIX.matcher(testo);
            if (matcher.matches()) {
                autore = matcher.group(1);
                testo = matcher.group(2);
            }
            out.add(new Commento(autore, testo, n.path("created_at").asText(null)));
        }
        return out;
    }

    public Commento addComment(Integer userStoryId, String testo, String autoreUsername) throws IOException, InterruptedException {
        JsonNode current = requestJson("GET", baseUrl + "/api/v1/userstories/" + userStoryId, null, true);
        int version = current.path("version").asInt();

        String testoConAutore = autoreUsername == null || autoreUsername.isBlank()
                ? testo
                : autoreUsername.toUpperCase() + ": " + testo;

        ObjectNode body = mapper.createObjectNode();
        body.put("version", version);
        body.put("comment", testoConAutore);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/userstories/" + userStoryId))
                .header("Authorization", "Bearer " + getAuthToken(false))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        http.send(request, HttpResponse.BodyHandlers.discarding());

        return new Commento(autoreUsername != null ? autoreUsername.toUpperCase() : botUsername, testo, null);
    }

    // --- internals -----------------------------------------------------

    private int getProjectId() throws IOException, InterruptedException {
        if (projectId != null) return projectId;
        synchronized (this) {
            if (projectId != null) return projectId;
            JsonNode project = requestJson("GET", baseUrl + "/api/v1/projects/by_slug?slug=" + urlEncode(projectSlug), null, true);
            projectId = project.path("id").asInt();
            return projectId;
        }
    }

    private int getNewStatusId(int project) throws IOException, InterruptedException {
        if (newStatusId != null) return newStatusId;
        synchronized (this) {
            if (newStatusId != null) return newStatusId;
            JsonNode projectDetail = requestJson("GET", baseUrl + "/api/v1/projects/" + project, null, true);
            JsonNode statuses = projectDetail.path("us_statuses");
            newStatusId = StreamSupport.stream(statuses.spliterator(), false)
                    .min(Comparator.comparingInt(s -> s.path("order").asInt()))
                    .map(s -> s.path("id").asInt())
                    .orElseThrow(() -> new IOException("Nessuno stato user-story trovato nel progetto Taiga " + project));
            return newStatusId;
        }
    }

    private synchronized String getAuthToken(boolean forceRefresh) throws IOException, InterruptedException {
        if (authToken != null && !forceRefresh) return authToken;
        if (baseUrl.isBlank() || botUsername.isBlank() || botPassword.isBlank()) {
            throw new IOException("Configurazione Taiga mancante: crea C:/opt/gdrsheet/config/taiga.properties " +
                    "(o /opt/gdrsheet/config/taiga.properties) con app.taiga.base-url, app.taiga.project-slug, " +
                    "app.taiga.bot-username, app.taiga.bot-password");
        }

        ObjectNode body = mapper.createObjectNode();
        body.put("type", "normal");
        body.put("username", botUsername);
        body.put("password", botPassword);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/auth"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new IOException("Autenticazione Taiga fallita: HTTP " + response.statusCode() + " - " + response.body());
        }
        authToken = mapper.readTree(response.body()).path("auth_token").asText();
        return authToken;
    }

    private JsonNode requestJson(String method, String url, String jsonBody, boolean retryOn401) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + getAuthToken(false))
                .header("Content-Type", "application/json");
        builder = switch (method) {
            case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody == null ? "" : jsonBody, StandardCharsets.UTF_8));
            case "PATCH" -> builder.method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody == null ? "" : jsonBody, StandardCharsets.UTF_8));
            default -> builder.GET();
        };
        HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401 && retryOn401) {
            getAuthToken(true);
            return requestJson(method, url, jsonBody, false);
        }
        if (response.statusCode() >= 300) {
            throw new IOException("Chiamata Taiga fallita (" + method + " " + url + "): HTTP " + response.statusCode() + " - " + response.body());
        }
        return mapper.readTree(response.body());
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
