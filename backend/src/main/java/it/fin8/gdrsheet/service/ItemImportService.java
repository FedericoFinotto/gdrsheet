package it.fin8.gdrsheet.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.dto.ImportJsonlResultDTO;
import it.fin8.gdrsheet.dto.UpdateItemRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Import bulk di item di compendio da un file JSONL (un oggetto JSON per riga),
 * secondo una mappatura {campo destinazione -> percorso sorgente nel JSON}.
 * <p>
 * Chiavi di mappatura ammesse:
 * - "nome"        -> sorgente scalare (es. "name")
 * - "descrizione" -> sorgente scalare (es. "description")
 * - "label.CHIAVE" -> sorgente scalare, lista (una riga label per elemento) o
 * "oggetto.*" (wildcard: tutte le chiavi di quell'oggetto NON già mappate
 * esplicitamente altrove, come "titolo: testo").
 * <p>
 * Pensato per l'import dei Talenti scaricati con scripts/dndtools-scraper, ma
 * generico rispetto al file e al tipo di item.
 */
@Service
public class ItemImportService {

    @Autowired
    private ItemService itemService;
    @Autowired
    private ObjectMapper objectMapper;

    public ImportJsonlResultDTO importJsonl(MultipartFile file, String mappingJson, TipoItem tipo) {
        if (tipo == null) throw new RuntimeException("Tipo item obbligatorio");

        Map<String, String> mapping;
        try {
            mapping = objectMapper.readValue(mappingJson, new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("Mapping non valido (atteso un oggetto JSON piatto {campo: sorgente}): " + e.getMessage());
        }

        int total = 0;
        int created = 0;
        List<ImportJsonlResultDTO.ImportRowErrorDTO> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                total++;
                try {
                    Map<String, Object> record = objectMapper.readValue(line, new TypeReference<Map<String, Object>>() {
                    });
                    UpdateItemRequest request = buildRequest(record, mapping, tipo);
                    itemService.createItem(request);
                    created++;
                } catch (Exception e) {
                    errors.add(new ImportJsonlResultDTO.ImportRowErrorDTO(total, e.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Errore lettura file: " + e.getMessage(), e);
        }

        return new ImportJsonlResultDTO(total, created, total - created, errors);
    }

    private UpdateItemRequest buildRequest(Map<String, Object> record, Map<String, String> mapping, TipoItem tipo) {
        String nome = null;
        String descrizione = null;
        List<UpdateItemRequest.LabelRowDTO> labels = new ArrayList<>();

        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String target = entry.getKey();
            List<String> values = resolveValues(record, entry.getValue(), mapping);
            if (values.isEmpty()) continue;

            if ("nome".equals(target)) {
                nome = values.get(0);
            } else if ("descrizione".equals(target)) {
                descrizione = values.get(0);
            } else if (target.startsWith("label.")) {
                String labelKey = target.substring("label.".length());
                for (String v : values) {
                    labels.add(new UpdateItemRequest.LabelRowDTO(labelKey, v));
                }
            } else {
                throw new IllegalArgumentException(
                        "Chiave di mapping non valida: '" + target + "' (usa 'nome', 'descrizione' o 'label.<NOME>')");
            }
        }

        return UpdateItemRequest.builder()
                .nome(nome)
                .descrizione(descrizione)
                .tipo(tipo)
                .labels(labels)
                .build();
    }

    /**
     * Risolve la sorgente (dot-path, es. "extraSections.Special") in una lista di stringhe:
     * - liste JSON -> un valore per elemento (es. "categories")
     * - "oggetto.*" -> "chiave: valore" per ogni entry di quell'oggetto non già mappata altrove
     * - scalare -> singolo valore
     * - assente/vuoto -> lista vuota (nessuna label creata)
     */
    private List<String> resolveValues(Map<String, Object> record, String source, Map<String, String> fullMapping) {
        if (source == null || source.isBlank()) return List.of();

        if (source.endsWith(".*")) {
            String objectPath = source.substring(0, source.length() - 2);
            Object parent = navigate(record, objectPath);
            if (!(parent instanceof Map<?, ?> parentMap)) return List.of();
            Set<String> excluded = siblingKeysExcludedFor(fullMapping, objectPath + ".");
            List<String> out = new ArrayList<>();
            for (Map.Entry<?, ?> e : parentMap.entrySet()) {
                String key = String.valueOf(e.getKey());
                if (excluded.contains(key)) continue;
                String val = e.getValue() == null ? "" : String.valueOf(e.getValue()).trim();
                if (val.isEmpty()) continue;
                out.add(key + ": " + val);
            }
            return out;
        }

        Object value = navigate(record, source);
        if (value == null) return List.of();
        if (value instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                if (o == null) continue;
                String s = String.valueOf(o).trim();
                if (!s.isEmpty()) out.add(s);
            }
            return out;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? List.of() : List.of(s);
    }

    /** Chiavi di "objectPathDot" (es. "extraSections.") già mappate esplicitamente da un'altra riga della mappatura. */
    private Set<String> siblingKeysExcludedFor(Map<String, String> mapping, String objectPathDot) {
        Set<String> excluded = new HashSet<>();
        for (String src : mapping.values()) {
            if (src != null && src.startsWith(objectPathDot) && !src.endsWith(".*")) {
                excluded.add(src.substring(objectPathDot.length()));
            }
        }
        return excluded;
    }

    private Object navigate(Map<String, Object> record, String dotPath) {
        Object current = record;
        for (String part : dotPath.split("\\.")) {
            if (!(current instanceof Map<?, ?> m)) return null;
            current = m.get(part);
        }
        return current;
    }
}
