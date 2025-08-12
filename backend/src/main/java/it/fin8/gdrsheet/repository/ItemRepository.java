package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.dto.ItemLivelloDTO;
import it.fin8.gdrsheet.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Integer> {

    List<Item> findItemsByTipo(TipoItem tipo);

    Item findItemById(Integer id);

    @Query("SELECT i FROM Item i WHERE i.id IN :ids")
    List<Item> findItemsByIds(@Param("ids") List<Integer> ids);


    @Query("SELECT i FROM Item i LEFT JOIN FETCH i.child c WHERE i.personaggio.id = :personaggioId")
    List<Item> findAllByPersonaggioIdWithChild(@Param("personaggioId") Integer id);

    Item findItemByNomeAndPersonaggio_Id(String name, Integer personaggioId);

    /**
     * Restituisce l'intera entità Item e il valore (livello) associato,
     * filtrando per tipo INCANTESIMO, label e lista di IDs.
     */
    @Query("""
                SELECT new it.fin8.gdrsheet.dto.ItemLivelloDTO(
                  i,
                  il.valore
                )
                FROM Item i
                JOIN i.labels il
                WHERE il.label = :label
                  AND i.tipo   = 'INCANTESIMO'
                  AND i.id     IN :ids
            """)
    List<ItemLivelloDTO> findIncantesimiWithLivelloByLabelAndIds(
            @Param("label") String label,
            @Param("ids") List<Integer> ids
    );

    @Query("""
              SELECT DISTINCT new it.fin8.gdrsheet.dto.ItemLivelloDTO(
                i,
                il.valore
              )
              FROM Item i
              JOIN i.labels il
              WHERE i.tipo = 'INCANTESIMO'
                AND il.label = :label
                AND il.valore = :livello
              ORDER BY il.valore ASC, i.nome ASC
            """)
    List<ItemLivelloDTO> findIncantesimiByLabelAndMaxLivello(
            @Param("label") String label,
            @Param("livello") Integer livello
    );


    /**
     * Restituisce l'intera entità Item e il valore (livello) associato,
     * filtrando per tipo INCANTESIMO, label e lista di IDs.
     */
    @Query("""
                SELECT new it.fin8.gdrsheet.dto.ItemLivelloDTO(
                  i,
                  il.valore
                )
                FROM Item i
                JOIN i.labels il
                WHERE il.label = :label
                  AND i.tipo   = 'AVANZAMENTO'
                  AND i.id     IN :ids
            """)
    List<ItemLivelloDTO> findAvanzamentiWithLivelloByLabelAndIds(
            @Param("label") String label,
            @Param("ids") List<Integer> ids
    );
}
