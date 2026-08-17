package it.fin8.gdrsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.fin8.gdrsheet.def.CardEditorItem;
import it.fin8.gdrsheet.def.TipoItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Abilitazione opt-in di una {@link CardEditorItem} per (mondo, tipo item): se non c'è una riga,
 * quella card/fold non è visibile in BaseItemEditor.vue per quel tipo in quel mondo. Stessa logica
 * "opt-in, seed fedele per i mondi esistenti" di {@link MondoTipoItemAbilitato}.
 */
@Getter
@Setter
@Entity
@Table(name = "mondo_tipo_item_card_abilitata")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MondoTipoItemCardAbilitata implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_mondo", nullable = false)
    private Mondo mondo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private TipoItem tipo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "card", nullable = false, length = 50)
    private CardEditorItem card;

}
