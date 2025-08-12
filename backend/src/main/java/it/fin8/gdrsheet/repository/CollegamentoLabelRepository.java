package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.CollegamentoLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollegamentoLabelRepository extends JpaRepository<CollegamentoLabel, Integer> {
}
