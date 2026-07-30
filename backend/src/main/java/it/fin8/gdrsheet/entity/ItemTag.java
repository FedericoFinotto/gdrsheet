package it.fin8.gdrsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Associazione pesata (oggetto, tag, peso) usata dai Randomizzatori.
 * <p>
 * Il peso vive qui e non sul tag: lo stesso tag può pesare diversamente su oggetti diversi.
 * {@link #categoria} è denormalizzata dal tag (che porta la label CATEGORIA=&lt;id&gt;) per
 * poter risolvere l'intera selezione con un solo scan di questa tabella.
 * <p>
 * Invariante: peso &gt; 0. "Peso 0" e "tag assente" sono la stessa cosa, quindi in quel caso
 * la riga non deve esistere (vincolo ck_item_tag_peso_positivo).
 */
@Getter
@Setter
@Entity
@Table(name = "item_tag")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@AllArgsConstructor
@NoArgsConstructor
public class ItemTag implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    /** Oggetto taggato. */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_item", nullable = false)
    private Item item;

    /** Item di tipo TAG. */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tag", nullable = false)
    private Item tag;

    /** Item di tipo CATEGORIA a cui appartiene il tag (denormalizzata). */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Item categoria;

    @NotNull
    @Column(name = "peso", nullable = false, precision = 10, scale = 3)
    private BigDecimal peso;
}
