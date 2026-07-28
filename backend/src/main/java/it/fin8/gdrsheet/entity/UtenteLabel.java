package it.fin8.gdrsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Coppie label/valore legate a un utente (preferenze, stato UI persistito...), stessa logica di
 * ItemLabel/PersonaggioLabel/CollegamentoLabel: nessuna colonna dedicata per ogni nuova cosa da
 * ricordare, solo una nuova riga con una label diversa. Primo utilizzo: l'ultimo mondo aperto
 * (vedi Constants.UTENTE_LABEL_ULTIMO_MONDO), ma pensata per ospitare altre preferenze future.
 */
@Getter
@Setter
@Entity
@Table(name = "utente_label")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class UtenteLabel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_utente", nullable = false)
    private Utente utente;

    @Column(name = "label", length = Integer.MAX_VALUE)
    private String label;

    @Column(name = "valore", length = Integer.MAX_VALUE)
    private String valore;
}
