package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.def.TipoItem;
import it.fin8.gdrsheet.entity.Collegamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CollegamentoRepository extends JpaRepository<Collegamento, Integer> {

    List<Collegamento> findAllByItemSource_Id(Integer id);

    /**
     * Figli (id/nome/tipo del target) per un insieme di item sorgente, filtrati per tipo target.
     * Proiezione leggera (niente JOIN FETCH di entità intere): usata per arricchire in batch gli
     * ItemDTO con i loro figli ATTACCO/TRASFORMAZIONE/FORMA senza scatenare lazy-load su
     * Item.child (non tutti gli item del flatten hanno il child eager-fetched).
     */
    @Query("""
            SELECT c.itemSource.id, c.itemTarget.id, c.itemTarget.nome, c.itemTarget.tipo
            FROM Collegamento c
            WHERE c.itemSource.id IN :sourceIds
              AND c.itemTarget.tipo IN :tipi
            """)
    List<Object[]> findFigliByTipo(@Param("sourceIds") List<Integer> sourceIds, @Param("tipi") List<TipoItem> tipi);

    List<Collegamento> findAllByItemTarget_Id(Integer id);

    /** Somma qty espliciti per ogni itemTarget (null = non contato). Solo righe con almeno un qty != null. */
    @Query("SELECT c.itemTarget.id, SUM(c.qty) " +
           "FROM Collegamento c " +
           "WHERE c.itemSource.id IN :sourceIds AND c.itemTarget.id IN :targetIds AND c.qty IS NOT NULL " +
           "GROUP BY c.itemTarget.id")
    List<Object[]> sumQtyByTargets(@Param("sourceIds") List<Integer> sourceIds,
                                   @Param("targetIds") List<Integer> targetIds);

    /** Restituisce i collegamenti con formulaQty valorizzata (JOIN FETCH per evitare LazyInit fuori transazione). */
    @Query("SELECT c FROM Collegamento c JOIN FETCH c.itemSource JOIN FETCH c.itemTarget WHERE c.itemSource.id IN :sourceIds AND c.itemTarget.id IN :targetIds AND c.formulaQty IS NOT NULL")
    List<Collegamento> findWithFormulaQty(@Param("sourceIds") List<Integer> sourceIds,
                                          @Param("targetIds") List<Integer> targetIds);
}
