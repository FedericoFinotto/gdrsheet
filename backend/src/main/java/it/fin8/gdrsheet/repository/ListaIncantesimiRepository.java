package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.ListaIncantesimi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ListaIncantesimiRepository extends JpaRepository<ListaIncantesimi, String> {
}
