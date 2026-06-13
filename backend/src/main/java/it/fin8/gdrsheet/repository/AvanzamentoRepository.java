package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.Avanzamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AvanzamentoRepository extends JpaRepository<Avanzamento, Integer> {
    List<Avanzamento> findAllByItemSource_Id(Integer idItemSource);
}
