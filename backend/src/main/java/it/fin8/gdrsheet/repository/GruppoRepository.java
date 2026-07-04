package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.Gruppo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GruppoRepository extends JpaRepository<Gruppo, Integer> {
    List<Gruppo> findAllByParty_IdOrderByNomeAsc(Integer partyId);
}
