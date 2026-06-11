package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.Collegamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CollegamentoRepository extends JpaRepository<Collegamento, Integer> {

    List<Collegamento> findAllByItemSource_Id(Integer id);

    List<Collegamento> findAllByItemTarget_Id(Integer id);
}
