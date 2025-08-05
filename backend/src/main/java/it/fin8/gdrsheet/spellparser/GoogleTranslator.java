package it.fin8.gdrsheet.spellparser;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class GoogleTranslator {
    public static String translate(String text, String from, String to) throws IOException {
        String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=" +
                from + "&tl=" + to + "&dt=t&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8);

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.connect();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String json = reader.readLine();
            // parsing manuale (struttura: [[[ "Traduzione", "originale", ... ]]])
            int start = json.indexOf("\"") + 1;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        }
    }
}
