package it.fin8.grdsheet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "ruolo")
public class Ruolo {
    @Id
    @ColumnDefault("nextval('ruolo_id_seq')")
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "descrizione", nullable = false, length = 64)
    private String descrizione;

}