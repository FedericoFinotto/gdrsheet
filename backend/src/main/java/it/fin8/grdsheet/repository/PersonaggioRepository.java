package it.fin8.grdsheet.repository;

import it.fin8.grdsheet.entity.Personaggio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonaggioRepository extends JpaRepository<Personaggio, Integer> {
}
