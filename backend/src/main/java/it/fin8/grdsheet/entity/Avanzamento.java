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
@Table(name = "avanzamento")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Avanzamento implements Serializable {
    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_item_source", nullable = false)
    private Item idItemSource;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_item_target", nullable = false)
    private Item idItemTarget;

    @NotNull
    @Column(name = "livello", nullable = false)
    private Integer livello;

}