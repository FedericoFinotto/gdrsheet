package it.fin8.gdrsheet.dto;

import it.fin8.gdrsheet.def.TipoItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ItemDTO {
    private Integer id;
    private String nome;
    private TipoItem tipo;
    private Boolean disabled;
    /**
     * Quantità (label QTA), default 1.
     */
    private Integer quantita;
    /**
     * Barriera (talento con label TIPO=BARRIERA): hp temporanei "blu".
     */
    private Boolean barriera;
    private Integer barrMax;
    private Integer barrCons;
    /**
     * True se l'item è collegato direttamente via FromCompendio (può essere scollegato dall'utente).
     */
    private Boolean scollegabile;
    /**
     * Utilizzi massimi (label UTILIZZI sull'item, globale). Null = nessun limite definito.
     */
    private Integer utilizziTotale;
    /**
     * Utilizzi consumati dal personaggio corrente (label UTILIZZI_USATI per-personaggio).
     */
    private Integer utilizziUsati;
    /** Peso unitario in kg (label PESO). Null se non impostato. */
    private Double peso;
    /** Peso max contenibile in kg (label CAPIENZA). Solo su CONTENITORE. Null altrimenti. */
    private Double capienza;
    /** Flag su CONTENITORE: include ARMA abilitate nel calcolo del contenuto. */
    private Boolean includiArmiAbilitate;
    /** Flag su CONTENITORE: include OGGETTO abilitati nel calcolo del contenuto. */
    private Boolean includiOggettiAbilitati;
    /** Flag su CONTENITORE: include CONSUMABILE abilitati nel calcolo del contenuto. */
    private Boolean includiConsumabiliAbilitati;
    /** Flag su CONTENITORE ("Tutto quello che pesa"): include qualsiasi item non disabilitato con
     *  peso, di qualunque tipo (implica gli altri INCLUDI_*). */
    private Boolean includiTuttiAbilitati;
    /** Attacchi (ATTACCO) collegati come figli di questo item, precalcolati per evitare una chiamata
     *  di dettaglio separata quando si espande la riga. Vuoto se nessuno. */
    private List<ChildRefDTO> figliAttacchi;
    /** Manuale di provenienza (label MANUALE_SP). Null se non impostato. */
    private String manuale;
    /** Descrittori Abilità (label DESCR_STR/DESCR_MAG/DESCR_SOP/DESCR_NAT), possono essere true insieme. */
    private Boolean descrStraordinaria;
    private Boolean descrMagica;
    private Boolean descrSoprannaturale;
    private Boolean descrNaturale;
}
