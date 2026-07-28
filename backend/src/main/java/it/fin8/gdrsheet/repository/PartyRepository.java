package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.Party;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartyRepository extends JpaRepository<Party, Integer> {
    List<Party> findAllByMondo_IdOrderByNomeAsc(Integer mondoId);
}
