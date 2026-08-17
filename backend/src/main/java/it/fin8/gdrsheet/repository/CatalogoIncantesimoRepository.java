package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.def.TipoCatalogoIncantesimo;
import it.fin8.gdrsheet.entity.CatalogoIncantesimo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogoIncantesimoRepository extends JpaRepository<CatalogoIncantesimo, Integer> {
    List<CatalogoIncantesimo> findAllByTipoOrderByValoreAsc(TipoCatalogoIncantesimo tipo);
    boolean existsByTipoAndValore(TipoCatalogoIncantesimo tipo, String valore);
}
