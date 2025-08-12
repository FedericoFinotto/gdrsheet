package it.fin8.gdrsheet.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "collegamento_label")
@AllArgsConstructor
@NoArgsConstructor
public class CollegamentoLabel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_collegamento", nullable = false)
    private Collegamento collegamento;

    @Column(name = "label", length = Integer.MAX_VALUE)
    private String label;

    @Column(name = "valore", length = Integer.MAX_VALUE)
    private String valore;

}