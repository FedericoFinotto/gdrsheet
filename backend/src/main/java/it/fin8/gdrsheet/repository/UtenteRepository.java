package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.Utente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UtenteRepository extends JpaRepository<Utente, Integer> {
    Optional<Utente> findByUsernameIgnoreCase(String username);
}
