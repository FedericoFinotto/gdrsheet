package it.fin8.gdrsheet.service;

import it.fin8.gdrsheet.config.Constants;
import it.fin8.gdrsheet.entity.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class UtilService {

    public String getItemLabel(Item item, String label) {
        for (ItemLabel itemLabel : item.getLabels()) {
            if (itemLabel.getLabel().equals(label)) {
                return itemLabel.getValore();
            }
        }
        return null;
    }

    public List<String> getItemLabels(Item item, String label) {
        List<String> result = new ArrayList<>();
        for (ItemLabel itemLabel : item.getLabels()) {
            if (itemLabel.getLabel().equals(label)) {
                result.add(itemLabel.getValore());
            }
        }
        return result;
    }

    public String getCollegamentoLabel(Collegamento coll, String label) {
        for (CollegamentoLabel collLabel : coll.getLabels()) {
            if (collLabel.getLabel().equals(label)) {
                return collLabel.getValore();
            }
        }
        return null;
    }

    public String getPersonaggioLabel(Personaggio p, String label) {
        for (PersonaggioLabel pLabel : p.getLabels()) {
            if (pLabel.getLabel().equals(label)) {
                return pLabel.getValore();
            }
        }
        return null;
    }

    /**
     * Ritorna il PRIMO collegamento da seguire da start verso un item del personaggio target.
     */
    public Optional<Collegamento> findRightConnectionLink(Item start, Integer targetPersonaggioId) {
        List<Collegamento> chain = findLinkPathToPersonaggio(start, targetPersonaggioId);
        return (chain == null || chain.isEmpty()) ? Optional.empty() : Optional.of(chain.get(0));
    }

    /**
     * Ritorna la catena di COLLEGAMENTI (non di item) da start fino a un item che appartiene al personaggio target.
     */
    public List<Collegamento> findLinkPathToPersonaggio(Item start, Integer targetPersonaggioId) {
        if (start == null) return null;

        Integer startId = start.getId();
        if (startId == null) return null;

        // caso limite: start già del personaggio target → nessun collegamento da percorrere
        if (belongsTo(start, targetPersonaggioId)) {
            return new ArrayList<>(); // path di link vuoto
        }

        Deque<Item> q = new ArrayDeque<>();
        q.add(start);

        Set<Integer> visited = new HashSet<>();
        visited.add(startId);

        // per ogni ITEM raggiunto, memorizzo da quale ITEM ci sono arrivato e tramite quale COLLEGAMENTO
        Map<Integer, Integer> prevItemId = new HashMap<>();
        Map<Integer, Collegamento> viaLink = new HashMap<>();

        Item found = null;

        while (!q.isEmpty()) {
            Item cur = q.removeFirst();
            Integer curId = cur.getId();

            for (Collegamento link : safeLinks(cur)) {
                Item parent = linkParent(link);
                Integer pid = parent.getId();
                if (pid == null || !visited.add(pid)) continue;

                prevItemId.put(pid, curId);
                viaLink.put(pid, link);

                if (belongsTo(parent, targetPersonaggioId)) {
                    found = parent;
                    break;
                }
                q.addLast(parent);
            }
            if (found != null) break;
        }

        if (found == null) return null;

        // ricostruisci la lista di COLLEGAMENTI dalla fine all'inizio
        List<Collegamento> links = new ArrayList<>();
        for (Integer at = found.getId(); at != null && !at.equals(startId); at = prevItemId.get(at)) {
            Collegamento l = viaLink.get(at);
            if (l == null) break;
            links.add(l);
        }
        Collections.reverse(links); // ora: primo hop, ..., ultimo hop
        return links;
    }

    /**
     * True se QUALCHE collegamento nella catena è DISABILITATO.
     */
    public boolean linkPathHasAnyDisabled(List<Collegamento> links) {
        if (links == null || links.isEmpty()) return false;
        for (Collegamento l : links) {
            if ("1".equals(safeLabel(l, Constants.ITEM_LABEL_DISABILITATO))) return true;
        }
        return false;
    }

    /* ===================== Helpers ===================== */

    private boolean belongsTo(Item item, Integer targetPersonaggioId) {
        try {
            return item.getPersonaggio() != null
                    && Objects.equals(item.getPersonaggio().getId(), targetPersonaggioId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Estrai i parent come collegamenti (rinomina se il tuo getter ha nome diverso).
     */
    private List<Collegamento> safeLinks(Item i) {
        try {
            List<Collegamento> ls = i.getParent();
            return ls == null ? Collections.emptyList() : ls;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Estrai l'item padre dal collegamento (rinomina se il tuo getter è diverso).
     */
    private Item linkParent(Collegamento c) {
        try {
            return c.getItemSource();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Adatta questo metodo se le label sul collegamento non sono accessibili come getLabel(key).
     */
    private String safeLabel(Collegamento c, String key) {
        try {
            String v = c.getLabel(key);                // <-- se esiste
            return v == null ? "" : v.trim();
        } catch (Exception e) {
            // Esempio alternativo:
            // try {
            //     return c.getLabels().stream()
            //         .filter(l -> key.equals(l.getLabel()))
            //         .map(ItemLabel::getValore)
            //         .findFirst().orElse("");
            // } catch (Exception ignore) { }
            return "";
        }
    }
}

