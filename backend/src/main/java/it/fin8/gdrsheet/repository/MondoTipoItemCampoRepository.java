package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.entity.MondoTipoItemCampo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MondoTipoItemCampoRepository extends JpaRepository<MondoTipoItemCampo, Integer> {
    List<MondoTipoItemCampo> findAllByMondo_IdAndTipoOrderByOrdineAsc(Integer idMondo, TipoItem tipo);
    void deleteByMondo_IdAndTipo(Integer idMondo, TipoItem tipo);
}
