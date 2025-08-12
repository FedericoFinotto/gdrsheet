package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.Personaggio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonaggioRepository extends JpaRepository<Personaggio, Integer> {
    @Query("SELECT p FROM Personaggio p " +
            " LEFT JOIN FETCH p.items i" +
            " LEFT JOIN FETCH i.child ic" +
            " LEFT JOIN FETCH ic.itemTarget it" +
            " LEFT JOIN FETCH it.modificatori m" +
            " WHERE p.id = :id")
    Optional<Personaggio> findByIdWithEntireGraph(@Param("id") Integer id);

    Personaggio findPersonaggioById(Integer idPersonaggio);
}
