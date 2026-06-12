package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.Mondo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MondoRepository extends JpaRepository<Mondo, Integer> {
}
