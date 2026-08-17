package it.fin8.gdrsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Metadati non ripetibili per (mondo, tipo item) — per ora solo il titolo della card che
 * raggruppa i "campi liberi" (equivalente a {@code campiLabelTitolo} passato oggi a
 * BaseItemEditor.vue dai wrapper "Editor/Tipi/*.vue"). Riga assente = nessun titolo, i campi
 * liberi appaiono inline senza una card dedicata.
 */
@Getter
@Setter
@Entity
@Table(name = "mondo_tipo_item_meta")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MondoTipoItemMeta implements Serializable {

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

    @Size(max = 200)
    @Column(name = "campi_titolo", length = 200)
    private String campiTitolo;

}
