package it.fin8.gdrsheet.def;

import lombok.Getter;

/**
 * Permesso di un utente su un mondo specifico (tabella permessi_mondo). MASTER è l'unico valore
 * oggi, ma la tabella è pensata per ospitarne altri in futuro (es. EDIT_STAT, per abilitare la
 * sola modifica delle stat_default di un mondo senza concedere il resto dei poteri da master) —
 * senza bisogno di una nuova tabella o di un nuovo meccanismo, solo un nuovo valore qui e i
 * relativi controlli in AuthzService.
 */
@Getter
public enum TipoPermessoMondo {
    MASTER("Master");

    private final String label;

    TipoPermessoMondo(String label) {
        this.label = label;
    }
}
