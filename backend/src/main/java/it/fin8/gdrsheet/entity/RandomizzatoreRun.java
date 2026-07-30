package it.fin8.gdrsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Storico di un'estrazione di un Randomizzatore.
 * Il risultato è JSON serializzato: non viene mai interrogato lato SQL, solo riletto e mostrato.
 */
@Getter
@Setter
@Entity
@Table(name = "randomizzatore_run")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@AllArgsConstructor
@NoArgsConstructor
public class RandomizzatoreRun implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    /** Item di tipo RANDOMIZZATORE che ha prodotto l'estrazione. */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_item", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utente")
    private Utente utente;

    @NotNull
    @Column(name = "eseguito_il", nullable = false)
    private LocalDateTime eseguitoIl;

    @Column(name = "risultato", length = Integer.MAX_VALUE)
    private String risultato;
}
