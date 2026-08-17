package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.MondoListaIncantesimiAbilitata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MondoListaIncantesimiAbilitataRepository extends JpaRepository<MondoListaIncantesimiAbilitata, Integer> {
    List<MondoListaIncantesimiAbilitata> findAllByMondo_Id(Integer idMondo);
    void deleteByMondo_IdAndListaIncantesimi_Codice(Integer idMondo, String codiceLista);
}
