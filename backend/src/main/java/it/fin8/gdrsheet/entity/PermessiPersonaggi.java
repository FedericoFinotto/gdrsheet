package it.fin8.gdrsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.fin8.gdrsheet.def.TipoPermessoPersonaggio;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "permessi_personaggi")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "permesso", nullable = false, length = 20)
    private TipoPermessoPersonaggio permesso = TipoPermessoPersonaggio.PROPRIETARIO;

    /**
     * Colonna legacy: per ora tenuta allineata a {@link #permesso}.
     */
    @Column(name = "tipo")
    private String tipo = TipoPermessoPersonaggio.PROPRIETARIO.name();

    public void setPermesso(TipoPermessoPersonaggio permesso) {
        this.permesso = permesso;
        this.tipo = permesso != null ? permesso.name() : null;
    }

}