package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.Collegamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollegamentoRepository extends JpaRepository<Collegamento, Integer> {

}
