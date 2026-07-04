package it.fin8.gdrsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Gruppo di personaggi all'interno di un party. L'appartenenza dei personaggi
 * è gestita via personaggio_label (GRUPPO = id del gruppo, CAPOGRUPPO = "1").
 */
@Getter
@Setter
@Entity
@Table(name = "gruppo")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Gruppo implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @Size(max = 100)
    @NotNull
    @Column(name = "nome", nullable = false, length = 100)
    private String nome;
}
