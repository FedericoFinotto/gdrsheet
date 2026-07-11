package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.def.TipoStat;
import it.fin8.gdrsheet.entity.Stat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatRepository extends JpaRepository<Stat, String> {
    List<Stat> findAllByOrderByLabelAsc();

    /** Tutte le stat di un tipo il cui id inizia con {@code prefisso} (es. "AR" -> AR00, AR01, ...). */
    List<Stat> findAllByTipoAndIdStartingWith(TipoStat tipo, String prefisso);
}
