package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.entity.MondoTipoItemMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MondoTipoItemMetaRepository extends JpaRepository<MondoTipoItemMeta, Integer> {
    Optional<MondoTipoItemMeta> findByMondo_IdAndTipo(Integer idMondo, TipoItem tipo);
    void deleteByMondo_IdAndTipo(Integer idMondo, TipoItem tipo);
}
