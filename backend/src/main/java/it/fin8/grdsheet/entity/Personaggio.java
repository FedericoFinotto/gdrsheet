package it.fin8.grdsheet.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "personaggio")
public class Personaggio {
    @Id
    @ColumnDefault("nextval('personaggio_id_seq')")
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "razza", nullable = false, length = 50)
    private String razza;

    @Column(name = "classe", nullable = false, length = 50)
    private String classe;

    @Column(name = "livello", nullable = false)
    private Integer livello;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utente_id", nullable = false)
    private Utente utente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mondo_id")
    private Mondo mondo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sistema_id")
    private Sistema sistema;

}