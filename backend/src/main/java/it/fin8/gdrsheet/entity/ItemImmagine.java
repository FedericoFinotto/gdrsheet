package it.fin8.gdrsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Immagine associata a un item. Il file vive su un host esterno: qui c'è solo il riferimento.
 * {@link #riferimentoEsterno} è l'identificatore del file presso l'host (per Cloudinary il
 * public_id): serve a cancellarlo davvero quando l'immagine viene rimossa dall'item.
 */
@Getter
@Setter
@Entity
@Table(name = "item_immagine")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@AllArgsConstructor
@NoArgsConstructor
public class ItemImmagine implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_item", nullable = false)
    private Item item;

    @NotNull
    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "riferimento_esterno", length = 500)
    private String riferimentoEsterno;

    @Column(name = "titolo", length = 200)
    private String titolo;

    @NotNull
    @Column(name = "ordine", nullable = false)
    private Integer ordine;

    @NotNull
    @Column(name = "caricata_il", nullable = false)
    private LocalDateTime caricataIl;
}
