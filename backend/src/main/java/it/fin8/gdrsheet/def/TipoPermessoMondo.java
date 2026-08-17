package it.fin8.gdrsheet.def;

import lombok.Getter;

/**
 * Permesso di un utente su un mondo specifico (tabella permessi_mondo). Tre valori,
 * completamente indipendenti tra loro (uno non implica gli altri, vanno assegnati singolarmente
 * anche se la stessa persona li possiede tutti e tre) — vedi i relativi controlli in
 * AuthzService:
 * <ul>
 *   <li>{@link #MASTER}: potere pieno "come il vecchio master" — creare party, creare utenti,
 *       associare utenti a un party, modificare qualunque item del mondo senza restrizioni
 *       (AuthzService.isMasterMondo, usato da ItemController/ItemImmagineController/
 *       RandomizzatoreController/PersonaggioService).</li>
 *   <li>{@link #STATS}: sola gestione delle stat_default del mondo (StatController,
 *       AuthzService.isStatsMondo) — pagina "Gestione Stat".</li>
 *   <li>{@link #PAGINE}: sola configurazione delle "pagine" del mondo (tipi item abilitati,
 *       card/campi dell'editor, catalogo scuole/liste incantesimi — MondoAdminController,
 *       AuthzService.isPagineMondo) — pagina "Permessi per mondo"/Editor per tipo.</li>
 * </ul>
 */
@Getter
public enum TipoPermessoMondo {
    MASTER("Master"),
    STATS("Statistiche"),
    PAGINE("Pagine");

    private final String label;

    TipoPermessoMondo(String label) {
        this.label = label;
    }
}
