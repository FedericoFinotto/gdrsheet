package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.def.TipoCatalogoIncantesimo;
import it.fin8.gdrsheet.entity.MondoCatalogoIncantesimoAbilitato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MondoCatalogoIncantesimoAbilitatoRepository extends JpaRepository<MondoCatalogoIncantesimoAbilitato, Integer> {
    List<MondoCatalogoIncantesimoAbilitato> findAllByMondo_IdAndTipo(Integer idMondo, TipoCatalogoIncantesimo tipo);
}
