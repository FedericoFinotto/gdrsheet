package it.fin8.gdrsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Catalogo globale dei codici lista/dominio incantesimi (es. SP_DRUID, SP_FIRE). Il catalogo è
 * unico e condiviso tra tutti i mondi: ciò che varia per mondo è solo quali codici sono
 * abilitati, vedi {@link MondoListaIncantesimiAbilitata}.
 */
@Getter
@Setter
@Entity
@Table(name = "lista_incantesimi")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ListaIncantesimi implements Serializable {

    @Id
    @Size(max = 50)
    @Column(name = "codice", nullable = false, length = 50)
    private String codice;

    @NotNull
    @Size(max = 100)
    @Column(name = "etichetta", nullable = false, length = 100)
    private String etichetta;

}
