package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.Personaggio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    List<Personaggio> findAllByParty_IdOrderByNomeAsc(Integer idParty);

    @Query("""
            SELECT p FROM Personaggio p JOIN p.labels l
            WHERE l.label = 'TIPO_PERSONAGGIO' AND l.valore = 'BANCA'
            ORDER BY p.nome
            """)
    List<Personaggio> findAllBanche();
}
