package it.fin8.gdrsheet.def;

import java.util.HashMap;
import java.util.Map;

/**
 * Progressioni standard degli slot incantesimo (spells/day base, D&D 3.5),
 * indicizzate per livello di classe. Ogni riga è l'array degli slot per
 * livello di incantesimo (indice 0 = trucchetti/livello 0).
 * <p>
 * Una sezione incantatore può riferirsi a una di queste progressioni invece di
 * compilare a mano la tabella SP_SLOT. Il valore speciale {@code CUSTOM} indica
 * che gli slot vanno letti dagli avanzamenti della classe (vecchio comportamento).
 */
public final class ProgressioneIncantesimi {

    private ProgressioneIncantesimi() {
    }

    public static final String CUSTOM = "CUSTOM";

    // tabelle: chiave = nome progressione, valore = slot[classLevel-1][spellLevel]
    private static final Map<String, int[][]> TABELLE = new HashMap<>();

    /** Slot base per il dato livello di classe (1..20). Array vuoto se non applicabile. */
    public static int[] slotsPerLivello(String progressione, int livelloClasse) {
        if (progressione == null) return new int[0];
        int[][] t = TABELLE.get(progressione.trim().toUpperCase());
        if (t == null) return new int[0];
        int idx = livelloClasse - 1;
        if (idx < 0 || idx >= t.length) return new int[0];
        return t[idx];
    }

    public static boolean isPreset(String progressione) {
        return progressione != null && TABELLE.containsKey(progressione.trim().toUpperCase());
    }

    /** Nomi delle progressioni preset disponibili. */
    public static java.util.Set<String> presets() {
        return TABELLE.keySet();
    }

    static {
        // MAGO (full caster, 9 livelli)
        TABELLE.put("MAGO", new int[][]{
                {3, 1},
                {4, 2},
                {4, 2, 1},
                {4, 3, 2},
                {4, 3, 2, 1},
                {4, 3, 3, 2},
                {4, 4, 3, 2, 1},
                {4, 4, 3, 3, 2},
                {4, 4, 4, 3, 2, 1},
                {4, 4, 4, 3, 3, 2},
                {4, 4, 4, 4, 3, 2, 1},
                {4, 4, 4, 4, 3, 3, 2},
                {4, 4, 4, 4, 4, 3, 2, 1},
                {4, 4, 4, 4, 4, 3, 3, 2},
                {4, 4, 4, 4, 4, 4, 3, 2, 1},
                {4, 4, 4, 4, 4, 4, 3, 3, 2},
                {4, 4, 4, 4, 4, 4, 4, 3, 2, 1},
                {4, 4, 4, 4, 4, 4, 4, 3, 3, 2},
                {4, 4, 4, 4, 4, 4, 4, 4, 3, 3},
                {4, 4, 4, 4, 4, 4, 4, 4, 4, 4},
        });

        // CHIERICO / DRUIDO (stessa progressione base; il dominio del chierico è una sezione a parte)
        int[][] chierico = {
                {3, 1},
                {4, 2},
                {4, 2, 1},
                {5, 3, 2},
                {5, 3, 2, 1},
                {5, 3, 3, 2},
                {6, 4, 3, 2, 1},
                {6, 4, 3, 3, 2},
                {6, 4, 4, 3, 2, 1},
                {6, 4, 4, 3, 3, 2},
                {6, 5, 4, 4, 3, 2, 1},
                {6, 5, 4, 4, 3, 3, 2},
                {6, 5, 5, 4, 4, 3, 2, 1},
                {6, 5, 5, 4, 4, 3, 3, 2},
                {6, 5, 5, 5, 4, 4, 3, 2, 1},
                {6, 5, 5, 5, 4, 4, 3, 3, 2},
                {6, 5, 5, 5, 5, 4, 4, 3, 2, 1},
                {6, 5, 5, 5, 5, 4, 4, 3, 3, 2},
                {6, 5, 5, 5, 5, 5, 4, 4, 3, 3},
                {6, 5, 5, 5, 5, 5, 4, 4, 4, 4},
        };
        TABELLE.put("CHIERICO", chierico);
        TABELLE.put("DRUIDO", chierico);

        // STREGONE (spontaneo, 9 livelli)
        TABELLE.put("STREGONE", new int[][]{
                {5, 3},
                {6, 4},
                {6, 5},
                {6, 6, 3},
                {6, 6, 4},
                {6, 6, 5, 3},
                {6, 6, 6, 4},
                {6, 6, 6, 5, 3},
                {6, 6, 6, 6, 4},
                {6, 6, 6, 6, 5, 3},
                {6, 6, 6, 6, 6, 4},
                {6, 6, 6, 6, 6, 5, 3},
                {6, 6, 6, 6, 6, 6, 4},
                {6, 6, 6, 6, 6, 6, 5, 3},
                {6, 6, 6, 6, 6, 6, 6, 4},
                {6, 6, 6, 6, 6, 6, 6, 5, 3},
                {6, 6, 6, 6, 6, 6, 6, 6, 4},
                {6, 6, 6, 6, 6, 6, 6, 6, 5, 3},
                {6, 6, 6, 6, 6, 6, 6, 6, 6, 4},
                {6, 6, 6, 6, 6, 6, 6, 6, 6, 6},
        });

        // BARDO (6 livelli)
        TABELLE.put("BARDO", new int[][]{
                {2},
                {3, 0},
                {3, 1},
                {3, 2, 0},
                {3, 3, 1},
                {3, 3, 2},
                {3, 3, 2, 0},
                {3, 3, 3, 1},
                {3, 3, 3, 2},
                {3, 3, 3, 2, 0},
                {3, 3, 3, 3, 1},
                {3, 3, 3, 3, 2},
                {3, 3, 3, 3, 2, 0},
                {4, 3, 3, 3, 3, 1},
                {4, 4, 3, 3, 3, 2},
                {4, 4, 4, 3, 3, 2, 0},
                {4, 4, 4, 4, 3, 3, 1},
                {4, 4, 4, 4, 4, 3, 2},
                {4, 4, 4, 4, 4, 4, 3},
                {4, 4, 4, 4, 4, 4, 4},
        });

        // RANGER (4 livelli, parte dal 4°)
        int[][] ranger = {
                {},
                {},
                {},
                {0},
                {0},
                {1},
                {1},
                {1, 0},
                {1, 1},
                {1, 1, 0},
                {1, 1, 1},
                {1, 1, 1, 0},
                {1, 1, 1, 1},
                {2, 1, 1, 1},
                {2, 2, 1, 1},
                {2, 2, 2, 1},
                {2, 2, 2, 1},
                {3, 2, 2, 1},
                {3, 3, 3, 2},
                {3, 3, 3, 3},
        };
        TABELLE.put("RANGER", ranger);
        // PALADINO: stessa progressione del ranger
        TABELLE.put("PALADINO", ranger);
    }
}
