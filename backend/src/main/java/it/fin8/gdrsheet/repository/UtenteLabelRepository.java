package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.UtenteLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UtenteLabelRepository extends JpaRepository<UtenteLabel, Integer> {
    Optional<UtenteLabel> findByUtente_IdAndLabel(Integer idUtente, String label);
}
