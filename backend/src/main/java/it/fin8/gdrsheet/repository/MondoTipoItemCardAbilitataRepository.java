package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.entity.MondoTipoItemCardAbilitata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MondoTipoItemCardAbilitataRepository extends JpaRepository<MondoTipoItemCardAbilitata, Integer> {
    List<MondoTipoItemCardAbilitata> findAllByMondo_IdAndTipo(Integer idMondo, TipoItem tipo);
    void deleteByMondo_IdAndTipo(Integer idMondo, TipoItem tipo);
}
