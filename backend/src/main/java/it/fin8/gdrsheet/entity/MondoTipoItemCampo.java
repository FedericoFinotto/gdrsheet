package it.fin8.gdrsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.fin8.gdrsheet.def.TipoCampoEditor;
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
 * Campo libero definito da un mondo per un tipo item: sostituisce, per (mondo, tipo), l'array
 * {@code CAMPI} oggi hardcoded nei wrapper "Editor/Tipi/*.vue" — un mondo può definirne di suoi,
 * anche mai esistiti in nessun editor. {@code opzioni} è JSON grezzo (lista di {value,label}) per
 * i tipi SELECT e per i campi multiValore con opzioni chiuse (MultiSelectField lato frontend);
 * resta null per input liberi.
 */
@Getter
@Setter
@Entity
@Table(name = "mondo_tipo_item_campo")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MondoTipoItemCampo implements Serializable {

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
    @Size(max = 50)
    @Column(name = "chiave", nullable = false, length = 50)
    private String chiave;

    @NotNull
    @Size(max = 200)
    @Column(name = "etichetta", nullable = false, length = 200)
    private String etichetta;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_campo", length = 20)
    private TipoCampoEditor tipoCampo;

    @Size(max = 300)
    @Column(name = "placeholder", length = 300)
    private String placeholder;

    @Column(name = "textarea", nullable = false)
    private boolean textarea;

    @Column(name = "multi_valore", nullable = false)
    private boolean multiValore;

    @Column(name = "html", nullable = false)
    private boolean html;

    @Column(name = "opzioni", columnDefinition = "text")
    private String opzioni;

    @Column(name = "ordine", nullable = false)
    private int ordine;

}
