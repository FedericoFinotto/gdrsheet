package it.fin8.gdrsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.fin8.gdrsheet.StatDefault;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "mondo")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Mondo implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Size(max = 100)
    @NotNull
    @Column(name = "descrizione", nullable = false, length = 100)
    private String descrizione;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sistema_id")
    private Sistema sistema;

    /** Se attivo, il costo in azioni (es. TEMPO_SP degli incantesimi) è mostrato coi glifi del
     * font icone Pathfinder2eActions invece che a testo. Nasce disattivato (default false). */
    @NotNull
    @Column(name = "mostra_simboli_azioni", nullable = false)
    private Boolean mostraSimboliAzioni = false;

    /** SLOT (default, storico) o MANA: vedi Constants.SISTEMA_INCANTESIMI_*. Se MANA, lo spellbook
     * usa un pool di mana condiviso (formulaManaIncantesimi) invece degli slot per livello, e il
     * costo di un incantesimo è pari al suo livello (i trucchetti/livello 0 sono gratuiti). */
    @NotNull
    @Column(name = "sistema_incantesimi", nullable = false, length = 10)
    private String sistemaIncantesimi = "SLOT";

    /** Formula del pool di mana totale (es. "@LVL*3+@CAR"), rilevante solo se sistemaIncantesimi=MANA.
     * Valutata come le altre formule bonus (vedi CardTabellaLivelli/getValoreFormula), stesse
     * variabili disponibili (@LVL, @CAR, ecc.). */
    @Column(name = "formula_mana_incantesimi", length = 255)
    private String formulaManaIncantesimi;

    /** Formula CD di default per gli incantesimi di questo mondo (es. "10+@LVL+%CAR"), con %CAR
     * sostituito dal modificatore della caratteristica da incantatore scelta sulla sezione. Se
     * assente, resta il calcolo storico "10 + caster level + modificatore". */
    @Column(name = "formula_cd_incantesimi", length = 255)
    private String formulaCdIncantesimi;

    /** SINGOLA o MULTIPLA (default, storico): vedi Constants.LISTA_INCANTESIMI_*. Se SINGOLA, una
     * sezione incantatore di questo mondo può avere una sola lista/dominio (niente unione di più
     * liste in una sezione, né più sezioni con liste diverse). */
    @NotNull
    @Column(name = "lista_incantesimi", nullable = false, length = 10)
    private String listaIncantesimi = "MULTIPLA";

    /** Se attivo (default, storico), la scheda mostra "CL: X" nello spellbook. Il caster level
     * continua comunque a essere calcolato internamente (serve per CD/slot/mana) anche se
     * disattivato: qui si nasconde solo la sua visualizzazione. */
    @NotNull
    @Column(name = "mostra_caster_level", nullable = false)
    private Boolean mostraCasterLevel = true;

    @OneToMany(mappedBy = "mondo", fetch = FetchType.LAZY)
    @JsonIgnoreProperties("mondo")
    private List<StatDefault> defaultStats;

    @OneToMany(mappedBy = "mondo", fetch = FetchType.LAZY)
    @JsonIgnoreProperties("mondo")
    private List<Item> items;

}