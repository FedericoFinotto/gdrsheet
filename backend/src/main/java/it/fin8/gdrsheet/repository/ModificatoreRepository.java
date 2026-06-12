package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.Modificatore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ModificatoreRepository extends JpaRepository<Modificatore, Integer> {
    List<Modificatore> findAllByItemIdIn(Collection<Integer> itemIds);

    @Query("""
            SELECT m FROM Modificatore m
            WHERE m.item.personaggio.id = :personaggioId
              AND m.stat.id IN :statIds
              AND NOT EXISTS (
                  SELECT 1 FROM ItemLabel il
                  WHERE il.item = m.item AND il.label = 'CC')
            """)
    List<Modificatore> findMonetePersonali(
            @Param("personaggioId") Integer personaggioId,
            @Param("statIds") Collection<String> statIds
    );

    @Query("""
            SELECT m FROM Modificatore m
            WHERE m.stat.id IN :statIds
              AND EXISTS (
                  SELECT 1 FROM ItemLabel il
                  WHERE il.item = m.item AND il.label = 'CC' AND il.valore = :cc)
            """)
    List<Modificatore> findMoneteConto(
            @Param("cc") String cc,
            @Param("statIds") Collection<String> statIds
    );
}
