package it.fin8.grdsheet.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Embeddable
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LivelloId implements Serializable {
    private static final long serialVersionUID = 7477846616246119107L;
    @NotNull
    @Column(name = "personaggio_id", nullable = false)
    private Integer personaggioId;

    @NotNull
    @Column(name = "item_classe_id", nullable = false)
    private Integer itemClasseId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        LivelloId entity = (LivelloId) o;
        return Objects.equals(this.itemClasseId, entity.itemClasseId) &&
                Objects.equals(this.personaggioId, entity.personaggioId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemClasseId, personaggioId);
    }

}