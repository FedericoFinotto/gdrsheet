package it.fin8.gdrsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Abilitazione opt-in di un codice {@link ListaIncantesimi} per un mondo, stessa logica di
 * {@link MondoTipoItemAbilitato} ma per le liste/domini incantesimi usabili nell'editor classe.
 */
@Getter
@Setter
@Entity
@Table(name = "mondo_lista_incantesimi_abilitata")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MondoListaIncantesimiAbilitata implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_mondo", nullable = false)
    private Mondo mondo;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codice_lista", nullable = false)
    private ListaIncantesimi listaIncantesimi;

}
