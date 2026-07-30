package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.ItemTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ItemTagRepository extends JpaRepository<ItemTag, Integer> {

    List<ItemTag> findByItem_Id(Integer idItem);

    /** Tag di un oggetto con tag e categoria già risolti: evita l'N+1 sui lazy in fase di estrazione. */
    @Query("SELECT it FROM ItemTag it JOIN FETCH it.tag JOIN FETCH it.categoria WHERE it.item.id = :idItem")
    List<ItemTag> findByItemIdFetch(@Param("idItem") Integer idItem);

    List<ItemTag> findByItem_IdIn(Collection<Integer> idItems);

    void deleteByItem_Id(Integer idItem);

    /**
     * Riallinea la categoria denormalizzata quando un TAG viene spostato di categoria.
     * Nativa e non JPQL: in JPQL non è lecito assegnare l'id di un'associazione
     * ({@code SET it.categoria.id = ...}), servirebbe l'entity intera.
     */
    @Modifying
    @Query(value = "UPDATE item_tag SET id_categoria = :idCategoria WHERE id_tag = :idTag",
            nativeQuery = true)
    int aggiornaCategoriaDenormalizzata(@Param("idTag") Integer idTag, @Param("idCategoria") Integer idCategoria);

    /**
     * Cuore del randomizzatore: in un solo scan restituisce gli oggetti candidati con il loro
     * peso combinato, già filtrati per tag scelti, categorie obbligatorie e visibilità
     * mondo/sistema.
     * <p>
     * Il conteggio {@code = :nTagScelti} impone che l'oggetto abbia TUTTI i tag scelti (uno per
     * categoria scelta); il {@code count(distinct ...)} impone che abbia almeno un tag per
     * ognuna delle categorie richieste come presenti.
     * <p>
     * Vengono restituiti entrambi i pesi combinati (somma e prodotto, quest'ultimo via
     * exp(sum(ln))) così il service può applicare la modalità configurata sul randomizzatore
     * senza una seconda query. ln(peso) è sempre definito perché peso &gt; 0 è un invariante.
     * <p>
     * Ritorna righe [idItem (Integer), pesoSomma (Double), pesoProdotto (Double)]: nessuna
     * entity Item viene caricata qui, solo il vincitore verrà poi materializzato.
     */
    @Query(value = """
            SELECT it.id_item                                                      AS id_item,
                   sum(it.peso) FILTER (WHERE it.id_tag IN (:tagScelti))           AS peso_somma,
                   exp(sum(ln(it.peso)) FILTER (WHERE it.id_tag IN (:tagScelti)))  AS peso_prodotto
            FROM item_tag it
            JOIN items i ON i.id = it.id_item
            WHERE i.personaggio_id IS NULL
              AND it.id_categoria IN (:categorieCoinvolte)
              AND (   (i.id_mondo IS NOT NULL AND i.id_mondo = :idMondo)
                   OR (i.id_mondo IS NULL AND i.id_sistema IS NOT NULL AND i.id_sistema = :idSistema)
                   OR (i.id_mondo IS NULL AND i.id_sistema IS NULL) )
            GROUP BY it.id_item
            HAVING count(*) FILTER (WHERE it.id_tag IN (:tagScelti)) = :nTagScelti
               AND count(DISTINCT it.id_categoria)
                   FILTER (WHERE it.id_categoria IN (:categoriePresenti)) = :nCategoriePresenti
            """, nativeQuery = true)
    List<Object[]> trovaCandidati(
            @Param("tagScelti") Collection<Integer> tagScelti,
            @Param("nTagScelti") long nTagScelti,
            @Param("categoriePresenti") Collection<Integer> categoriePresenti,
            @Param("nCategoriePresenti") long nCategoriePresenti,
            @Param("categorieCoinvolte") Collection<Integer> categorieCoinvolte,
            @Param("idMondo") Integer idMondo,
            @Param("idSistema") Integer idSistema
    );
}
