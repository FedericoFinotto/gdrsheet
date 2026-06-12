package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.PermessiParty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermessiPartyRepository extends JpaRepository<PermessiParty, Integer> {
    List<PermessiParty> findAllByIdUtente_Id(Integer idUtente);

    List<PermessiParty> findAllByIdParty_Id(Integer idParty);

    boolean existsByIdUtente_IdAndIdParty_Id(Integer idUtente, Integer idParty);
}
