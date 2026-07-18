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

    /**
     * Collegamenti verso item di tipo EFFETTO, tra tutti gli item di un personaggio (JOIN FETCH
     * per leggere nome sorgente/bersaglio e la label CONDIZIONE senza LazyInit fuori transazione).
     */
    @Query("SELECT c FROM Collegamento c JOIN FETCH c.itemSource JOIN FETCH c.itemTarget " +
           "WHERE c.itemSource.id IN :sourceIds AND c.itemTarget.tipo = it.fin8.gdrsheet.def.TipoItem.EFFETTO")
    List<Collegamento> findEffettiByItemSourceIds(@Param("sourceIds") List<Integer> sourceIds);

    /**
     * Risale il grafo Collegamento da un item toccato fino a tutte le radici (item con
     * personaggio_id valorizzato) raggiungibili, direttamente o indirettamente — un item di
     * compendio condiviso può comparire come figlio sotto le schede di più personaggi. Usata per
     * invalidare la cache di TUTTI i personaggi coinvolti quando quell'item viene modificato,
     * anche se la modifica parte dalla scheda di uno solo di loro.
     */
    @Query(value = """
            WITH RECURSIVE risalita(item_id) AS (
                SELECT id_item_source FROM collegamento WHERE id_item_target = :itemId
                UNION
                SELECT c.id_item_source
                FROM collegamento c
                JOIN risalita r ON c.id_item_target = r.item_id
            )
            SELECT DISTINCT i.personaggio_id
            FROM items i
            WHERE i.personaggio_id IS NOT NULL
              AND i.id IN (SELECT item_id FROM risalita UNION SELECT :itemId)
            """, nativeQuery = true)
    List<Integer> findPersonaggiRaggiungibiliDaItem(@Param("itemId") Integer itemId);
}
