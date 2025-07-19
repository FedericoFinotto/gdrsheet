package it.fin8.grdsheet.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "utente")
public class Utente {
    @Id
    @ColumnDefault("nextval('utente_id_seq')")
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "descrizione", nullable = false, length = 100)
    private String descrizione;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ruolo_id", nullable = false)
    private Ruolo ruolo;

}