package it.fin8.grdsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "default_item_label")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DefaultItemLabel implements Serializable {
    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_mondo", nullable = false)
    private Mondo idMondo;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_item_tipo", nullable = false)
    private ItemTipo idItemTipo;

    @Column(name = "label", length = Integer.MAX_VALUE)
    private String label;

}