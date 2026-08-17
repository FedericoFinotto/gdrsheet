package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.entity.MondoTipoItemAbilitato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MondoTipoItemAbilitatoRepository extends JpaRepository<MondoTipoItemAbilitato, Integer> {
    List<MondoTipoItemAbilitato> findAllByMondo_Id(Integer idMondo);
    boolean existsByMondo_IdAndTipo(Integer idMondo, TipoItem tipo);
    void deleteByMondo_IdAndTipo(Integer idMondo, TipoItem tipo);
}
