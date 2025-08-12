package it.fin8.gdrsheet.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "personaggio_label")
public class PersonaggioLabel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_personaggio", nullable = false)
    private Personaggio personaggio;

    @Column(name = "label", length = Integer.MAX_VALUE)
    private String label;

    @Column(name = "valore", length = Integer.MAX_VALUE)
    private String valore;

}