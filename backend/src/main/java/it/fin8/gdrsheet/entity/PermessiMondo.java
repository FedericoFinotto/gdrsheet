package it.fin8.gdrsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.fin8.gdrsheet.def.TipoPermessoMondo;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "permessi_mondo")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PermessiMondo {
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
    @JoinColumn(name = "id_mondo", nullable = false)
    private Mondo idMondo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "permesso", nullable = false, length = 30)
    private TipoPermessoMondo permesso;
}
