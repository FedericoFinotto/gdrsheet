package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.PersonaggioLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PersonaggioLabelRepository extends JpaRepository<PersonaggioLabel, Integer> {

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO personaggio_label (id_personaggio, label, valore)
            VALUES (:idPersonaggio, :label, :valore)
            ON CONFLICT (id_personaggio, label)
            DO UPDATE SET valore = EXCLUDED.valore
            """, nativeQuery = true)
    public void upsertLabel(
            @Param("idPersonaggio") Integer idPersonaggio,
            @Param("label") String label,
            @Param("valore") String valore
    );
}