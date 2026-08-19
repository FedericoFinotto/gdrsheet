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

    @OneToMany(mappedBy = "mondo", fetch = FetchType.LAZY)
    @JsonIgnoreProperties("mondo")
    private List<StatDefault> defaultStats;

    @OneToMany(mappedBy = "mondo", fetch = FetchType.LAZY)
    @JsonIgnoreProperties("mondo")
    private List<Item> items;

}