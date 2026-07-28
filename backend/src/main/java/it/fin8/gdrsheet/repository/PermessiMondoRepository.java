package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.def.TipoPermessoMondo;
import it.fin8.gdrsheet.entity.PermessiMondo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermessiMondoRepository extends JpaRepository<PermessiMondo, Integer> {
    List<PermessiMondo> findAllByIdUtente_Id(Integer idUtente);

    List<PermessiMondo> findAllByIdUtente_IdAndPermesso(Integer idUtente, TipoPermessoMondo permesso);

    List<PermessiMondo> findAllByIdMondo_Id(Integer idMondo);

    Optional<PermessiMondo> findByIdUtente_IdAndIdMondo_IdAndPermesso(Integer idUtente, Integer idMondo, TipoPermessoMondo permesso);

    boolean existsByIdUtente_IdAndIdMondo_IdAndPermesso(Integer idUtente, Integer idMondo, TipoPermessoMondo permesso);

    void deleteByIdUtente_IdAndIdMondo_IdAndPermesso(Integer idUtente, Integer idMondo, TipoPermessoMondo permesso);
}
