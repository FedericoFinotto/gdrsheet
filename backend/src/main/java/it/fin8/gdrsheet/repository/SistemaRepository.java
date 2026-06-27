package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.Sistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SistemaRepository extends JpaRepository<Sistema, Integer> {
}
