package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.Stat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatRepository extends JpaRepository<Stat, String> {
    List<Stat> findAllByOrderByLabelAsc();
}
