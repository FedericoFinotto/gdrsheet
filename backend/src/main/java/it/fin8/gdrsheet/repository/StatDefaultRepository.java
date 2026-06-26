package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.StatDefault;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatDefaultRepository extends JpaRepository<StatDefault, Integer> {
    List<StatDefault> findAllByMondo_Id(Integer mondoId);
}
