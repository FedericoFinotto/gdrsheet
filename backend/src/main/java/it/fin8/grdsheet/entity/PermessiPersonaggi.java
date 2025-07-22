package it.fin8.grdsheet.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "permessi_personaggi")
public class PermessiPersonaggi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_utente", nullable = false)
    private Utente idUtente;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_personaggio", nullable = false)
    private Personaggio idPersonaggio;

}