package it.fin8.grdsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "livello")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Livello implements Serializable {
    @EmbeddedId
    private LivelloId id;

    @MapsId("personaggioId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "personaggio_id", nullable = false)
    private Personaggio personaggio;

    @MapsId("itemClasseId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_classe_id", nullable = false)
    private Item itemClasse;

}