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
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Abilitazione opt-in di un {@link TipoItem} per un mondo: se non c'è una riga per
 * (mondo, tipo), quel tipo item non è utilizzabile in quel mondo. I mondi esistenti al momento
 * dell'introduzione di questa tabella sono stati popolati con tutti i tipi per non cambiare
 * comportamento (vedi migration db.changelog-13.0.xml); i mondi nuovi partono senza nulla e
 * devono abilitare esplicitamente ciò che usano.
 */
@Getter
@Setter
@Entity
@Table(name = "mondo_tipo_item_abilitato")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MondoTipoItemAbilitato implements Serializable {

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

}
