package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.PermessiPersonaggi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermessiPersonaggiRepository extends JpaRepository<PermessiPersonaggi, Integer> {
    List<PermessiPersonaggi> findAllByIdUtente_Id(Integer idUtente);

    List<PermessiPersonaggi> findAllByIdPersonaggio_Id(Integer idPersonaggio);
}
