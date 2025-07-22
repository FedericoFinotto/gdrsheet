package it.fin8.grdsheet.entity;

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

@Getter
@Setter
@Entity
@Table(name = "ruolo")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Ruolo implements Serializable {
    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Size(max = 64)
    @NotNull
    @Column(name = "descrizione", nullable = false, length = 64)
    private String descrizione;

}