package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.PartyLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartyLabelRepository extends JpaRepository<PartyLabel, Integer> {
    List<PartyLabel> findAllByParty_Id(Integer idParty);

    Optional<PartyLabel> findByParty_IdAndLabel(Integer idParty, String label);

    List<PartyLabel> findAllByParty_IdInAndLabel(List<Integer> idParty, String label);
}
