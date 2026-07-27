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

    boolean existsByNomeIgnoreCaseAndTipoAndPersonaggioIsNull(String nome, TipoItem tipo);

    Item findItemById(Integer id);

    @Query("""
            SELECT i FROM Item i
            LEFT JOIN FETCH i.labels
            WHERE i.tipo = it.fin8.gdrsheet.def.TipoItem.NOTIZIA
              AND i.personaggio IS NULL
            """)
    List<Item> findAllNotizie();

    /** Notizie di un mondo specifico, per la ricerca profonda del party (che vive in quel mondo). */
    @Query("""
            SELECT i FROM Item i
            LEFT JOIN FETCH i.labels
            WHERE i.tipo = it.fin8.gdrsheet.def.TipoItem.NOTIZIA
              AND i.mondo.id = :mondoId
            """)
    List<Item> findNotizieByMondoId(@Param("mondoId") Integer mondoId);

    @Query("SELECT i FROM Item i WHERE i.id IN :ids")
    List<Item> findItemsByIds(@Param("ids") List<Integer> ids);


    @Query("SELECT i FROM Item i LEFT JOIN FETCH i.child c WHERE i.personaggio.id = :personaggioId")
    List<Item> findAllByPersonaggioIdWithChild(@Param("personaggioId") Integer id);

    Item findItemByNomeAndPersonaggio_Id(String name, Integer personaggioId);

    // Cerca sia nel nome sia nella label EN_NAME (nome originale inglese), non solo nel nome.
    @Query("""
            SELECT i FROM Item i
            WHERE lower(i.nome) LIKE lower(concat('%', :nome, '%'))
               OR EXISTS (SELECT 1 FROM ItemLabel ien
                          WHERE ien.item = i AND ien.label = 'EN_NAME'
                            AND lower(ien.valore) LIKE lower(concat('%', :nome, '%')))
            ORDER BY i.nome ASC
            """)
    List<Item> findTop20ByNomeOrEnNameContainingIgnoreCase(@Param("nome") String nome, org.springframework.data.domain.Pageable pageable);

    /**
     * Ricerca profonda tra gli item di compendio (personaggio IS NULL): nome, descrizione, valore
     * di una label globale, o nota di un modificatore. Pre-filtro lato DB (a differenza della
     * ricerca profonda per personaggio, che opera su un albero già in memoria): il compendio ha
     * migliaia di item, qui serve la query a fare il lavoro pesante invece di caricarli tutti.
     * Il match esatto/lo snippet vengono ricalcolati in Java solo sui risultati (pochi).
     */
    @Query("""
            SELECT DISTINCT i FROM Item i
            WHERE i.personaggio IS NULL
              AND (
                lower(i.nome) LIKE lower(concat('%', :q, '%'))
                OR lower(i.descrizione) LIKE lower(concat('%', :q, '%'))
                OR EXISTS (SELECT 1 FROM ItemLabel il WHERE il.item = i AND il.personaggio IS NULL
                           AND lower(il.valore) LIKE lower(concat('%', :q, '%')))
                OR EXISTS (SELECT 1 FROM Modificatore m WHERE m.item = i
                           AND lower(m.nota) LIKE lower(concat('%', :q, '%')))
              )
            ORDER BY i.nome
            """)
    List<Item> searchCompendioDeep(@Param("q") String q, org.springframework.data.domain.Pageable pageable);

    /**
     * Come {@link #searchCompendioDeep}, ma pre-filtrata lato DB su un insieme di id già noto
     * (tipicamente {@code findReachableItemIds}) invece che su tutto il compendio: usata dalla
     * ricerca profonda di un personaggio, per evitare di dover ricostruire l'intera scheda
     * (flatten, livelli, classi...) solo per cercare del testo.
     */
    @Query("""
            SELECT DISTINCT i FROM Item i
            WHERE i.id IN :ids
              AND (
                lower(i.nome) LIKE lower(concat('%', :q, '%'))
                OR lower(i.descrizione) LIKE lower(concat('%', :q, '%'))
                OR EXISTS (SELECT 1 FROM ItemLabel il WHERE il.item = i AND il.personaggio IS NULL
                           AND lower(il.valore) LIKE lower(concat('%', :q, '%')))
                OR EXISTS (SELECT 1 FROM Modificatore m WHERE m.item = i
                           AND lower(m.nota) LIKE lower(concat('%', :q, '%')))
              )
            """)
    List<Item> searchByIdsDeep(@Param("ids") java.util.Collection<Integer> ids, @Param("q") String q);

    @Query("SELECT i FROM Item i JOIN i.labels il WHERE il.label = 'CC' AND il.valore = :cc")
    List<Item> findContiByCc(@Param("cc") String cc);

    @Query("""
            SELECT i FROM Item i
            WHERE i.personaggio IS NULL
              AND (:tipo IS NULL OR i.tipo = :tipo)
              AND (:nome = '' OR lower(i.nome) LIKE lower(concat('%', :nome, '%'))
                   OR EXISTS (SELECT 1 FROM ItemLabel ien
                              WHERE ien.item = i AND ien.label = 'EN_NAME'
                                AND lower(ien.valore) LIKE lower(concat('%', :nome, '%'))))
              AND (
                i.tipo IN (it.fin8.gdrsheet.def.TipoItem.INCANTESIMO,
                           it.fin8.gdrsheet.def.TipoItem.CLASSE,
                           it.fin8.gdrsheet.def.TipoItem.RAZZA,
                           it.fin8.gdrsheet.def.TipoItem.LINGUA,
                           it.fin8.gdrsheet.def.TipoItem.COMP)
                OR EXISTS (SELECT 1 FROM ItemLabel il
                           WHERE il.item = i AND il.label = 'COMPENDIO'
                             AND lower(il.valore) IN ('true', '1'))
              )
            ORDER BY i.nome
            """)
    org.springframework.data.domain.Page<Item> findCompendio(
            @Param("nome") String nome,
            @Param("tipo") TipoItem tipo,
            org.springframework.data.domain.Pageable pageable
    );

    // Come sopra, filtrata anche per tipo.
    @Query("""
            SELECT i FROM Item i
            WHERE i.tipo = :tipo
              AND (lower(i.nome) LIKE lower(concat('%', :nome, '%'))
                   OR EXISTS (SELECT 1 FROM ItemLabel ien
                              WHERE ien.item = i AND ien.label = 'EN_NAME'
                                AND lower(ien.valore) LIKE lower(concat('%', :nome, '%'))))
            ORDER BY i.nome ASC
            """)
    List<Item> findTop20ByNomeOrEnNameContainingIgnoreCaseAndTipo(@Param("nome") String nome, @Param("tipo") TipoItem tipo, org.springframework.data.domain.Pageable pageable);

    /** Compendio senza filtro COMPENDIO — visibile solo ad admin/master. */
    @Query("""
            SELECT i FROM Item i
            WHERE i.personaggio IS NULL
              AND (:tipo IS NULL OR i.tipo = :tipo)
              AND (:nome = '' OR lower(i.nome) LIKE lower(concat('%', :nome, '%'))
                   OR EXISTS (SELECT 1 FROM ItemLabel ien
                              WHERE ien.item = i AND ien.label = 'EN_NAME'
                                AND lower(ien.valore) LIKE lower(concat('%', :nome, '%'))))
              AND (:idMondo IS NULL OR (i.mondo IS NOT NULL AND i.mondo.id = :idMondo))
            ORDER BY i.nome
            """)
    org.springframework.data.domain.Page<Item> findCompendioAll(
            @Param("nome") String nome,
            @Param("tipo") TipoItem tipo,
            @Param("idMondo") Integer idMondo,
            org.springframework.data.domain.Pageable pageable
    );

    /** Compendio con visibilità per mondo/sistema dell'utente. */
    @Query("""
            SELECT i FROM Item i
            WHERE i.personaggio IS NULL
              AND (:tipo IS NULL OR i.tipo = :tipo)
              AND (:nome = '' OR lower(i.nome) LIKE lower(concat('%', :nome, '%'))
                   OR EXISTS (SELECT 1 FROM ItemLabel ien
                              WHERE ien.item = i AND ien.label = 'EN_NAME'
                                AND lower(ien.valore) LIKE lower(concat('%', :nome, '%'))))
              AND (:idMondo IS NULL OR (i.mondo IS NOT NULL AND i.mondo.id = :idMondo))
              AND (
                i.tipo IN (it.fin8.gdrsheet.def.TipoItem.INCANTESIMO,
                           it.fin8.gdrsheet.def.TipoItem.CLASSE,
                           it.fin8.gdrsheet.def.TipoItem.RAZZA,
                           it.fin8.gdrsheet.def.TipoItem.LINGUA,
                           it.fin8.gdrsheet.def.TipoItem.COMP)
                OR EXISTS (SELECT 1 FROM ItemLabel il
                           WHERE il.item = i AND il.label = 'COMPENDIO'
                             AND lower(il.valore) IN ('true', '1'))
              )
              AND (
                (i.mondo IS NOT NULL AND i.mondo.id IN :mondoIds)
                OR (i.mondo IS NULL AND i.sistema IS NOT NULL AND i.sistema.id IN :sistemaIds)
                OR (i.mondo IS NULL AND i.sistema IS NULL)
              )
            ORDER BY i.nome
            """)
    org.springframework.data.domain.Page<Item> findCompendioForUser(
            @Param("nome") String nome,
            @Param("tipo") TipoItem tipo,
            @Param("idMondo") Integer idMondo,
            @Param("mondoIds") List<Integer> mondoIds,
            @Param("sistemaIds") List<Integer> sistemaIds,
            org.springframework.data.domain.Pageable pageable
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

    /** Restituisce (id, tipo) per gli item con id nella lista data (es. per calcolaPeso). */
    @Query("SELECT i.id, i.tipo FROM Item i WHERE i.id IN :ids")
    List<Object[]> findIdAndTipoByIds(@Param("ids") java.util.Collection<Integer> ids);

    /**
     * Tutti gli id degli item "del personaggio": quelli intestati direttamente (FromCompendio,
     * PreparedSpell, Livelli, ...) più TUTTI i loro discendenti collegati via Collegamento, a
     * qualunque profondità (contenitori dentro contenitori inclusi). Usata da PartyService#giveItem
     * per trovare un item ovunque si trovi nell'inventario di origine (anche dentro un
     * CONTENITORE, non solo come figlio diretto del FromCompendio).
     */
    @Query(value = """
            WITH RECURSIVE reachable(id) AS (
                SELECT id FROM items WHERE personaggio_id = :personaggioId
                UNION
                SELECT c.id_item_target FROM collegamento c
                JOIN reachable r ON c.id_item_source = r.id
            )
            SELECT id FROM reachable
            """, nativeQuery = true)
    List<Integer> findReachableItemIds(@Param("personaggioId") Integer personaggioId);

    /**
     * Quest radice di un personaggio (ambito QUEST_SCOPE=PERSONAGGIO): intestate direttamente a lui.
     * Esclude le sotto-quest (già collegate come figlie di un'altra QUEST): una quest può avere
     * mondo/personaggio ereditati dal contesto di creazione senza per questo essere una radice.
     */
    @Query("""
            SELECT i FROM Item i
            WHERE i.tipo = it.fin8.gdrsheet.def.TipoItem.QUEST
              AND i.personaggio.id = :personaggioId
              AND NOT EXISTS (
                  SELECT 1 FROM Collegamento c
                  WHERE c.itemTarget = i AND c.itemSource.tipo = it.fin8.gdrsheet.def.TipoItem.QUEST
              )
            """)
    List<Item> findQuestByPersonaggioId(@Param("personaggioId") Integer personaggioId);

    /**
     * Come {@link #findQuestByPersonaggioId}, ma filtrata sullo stato di archiviazione (label
     * QUEST_ARCHIVIATA): usata dal caricamento "normale" delle sezioni quest, che mostra solo le
     * non archiviate finché l'utente non attiva esplicitamente il filtro "solo archiviate". La
     * ricerca profonda continua invece a usare {@link #findQuestByPersonaggioId} senza filtro:
     * un'archiviata deve restare trovabile.
     */
    @Query("""
            SELECT i FROM Item i
            WHERE i.tipo = it.fin8.gdrsheet.def.TipoItem.QUEST
              AND i.personaggio.id = :personaggioId
              AND NOT EXISTS (
                  SELECT 1 FROM Collegamento c
                  WHERE c.itemTarget = i AND c.itemSource.tipo = it.fin8.gdrsheet.def.TipoItem.QUEST
              )
              AND (
                (:archiviate = true AND EXISTS (SELECT 1 FROM ItemLabel ila WHERE ila.item = i
                    AND ila.label = 'QUEST_ARCHIVIATA' AND ila.valore = '1'))
                OR (:archiviate = false AND NOT EXISTS (SELECT 1 FROM ItemLabel ila WHERE ila.item = i
                    AND ila.label = 'QUEST_ARCHIVIATA' AND ila.valore = '1'))
              )
            """)
    List<Item> findQuestByPersonaggioIdArchiviata(@Param("personaggioId") Integer personaggioId, @Param("archiviate") boolean archiviate);

    /** Quest radice di un party (label QUEST_PARTY = id del party). Esclude le sotto-quest. */
    @Query("""
            SELECT i FROM Item i JOIN i.labels il
            WHERE i.tipo = it.fin8.gdrsheet.def.TipoItem.QUEST
              AND i.personaggio IS NULL
              AND il.label = 'QUEST_PARTY' AND il.valore = :partyId
              AND NOT EXISTS (
                  SELECT 1 FROM Collegamento c
                  WHERE c.itemTarget = i AND c.itemSource.tipo = it.fin8.gdrsheet.def.TipoItem.QUEST
              )
            """)
    List<Item> findQuestByPartyId(@Param("partyId") String partyId);

    /** Come {@link #findQuestByPartyId}, filtrata per stato di archiviazione: vedi la nota su {@link #findQuestByPersonaggioIdArchiviata}. */
    @Query("""
            SELECT i FROM Item i JOIN i.labels il
            WHERE i.tipo = it.fin8.gdrsheet.def.TipoItem.QUEST
              AND i.personaggio IS NULL
              AND il.label = 'QUEST_PARTY' AND il.valore = :partyId
              AND NOT EXISTS (
                  SELECT 1 FROM Collegamento c
                  WHERE c.itemTarget = i AND c.itemSource.tipo = it.fin8.gdrsheet.def.TipoItem.QUEST
              )
              AND (
                (:archiviate = true AND EXISTS (SELECT 1 FROM ItemLabel ila WHERE ila.item = i
                    AND ila.label = 'QUEST_ARCHIVIATA' AND ila.valore = '1'))
                OR (:archiviate = false AND NOT EXISTS (SELECT 1 FROM ItemLabel ila WHERE ila.item = i
                    AND ila.label = 'QUEST_ARCHIVIATA' AND ila.valore = '1'))
              )
            """)
    List<Item> findQuestByPartyIdArchiviata(@Param("partyId") String partyId, @Param("archiviate") boolean archiviate);

    /**
     * Quest radice di un intero mondo: visibili a tutti i party di quel mondo. Esclude le
     * sotto-quest — a differenza di PERSONAGGIO/PARTY (marcate da un segnale esplicito solo in
     * creazione radice), il mondo viene ereditato automaticamente da QUALUNQUE item creato nel
     * contesto di un personaggio/party, incluse le sotto-quest: senza questa esclusione ogni
     * sotto-quest di una quest radice apparirebbe anche come quest di mondo indipendente.
     */
    @Query("""
            SELECT i FROM Item i
            WHERE i.tipo = it.fin8.gdrsheet.def.TipoItem.QUEST
              AND i.personaggio IS NULL
              AND i.mondo.id = :mondoId
              AND NOT EXISTS (SELECT 1 FROM ItemLabel il2 WHERE il2.item = i AND il2.label = 'QUEST_PARTY')
              AND NOT EXISTS (
                  SELECT 1 FROM Collegamento c
                  WHERE c.itemTarget = i AND c.itemSource.tipo = it.fin8.gdrsheet.def.TipoItem.QUEST
              )
            """)
    List<Item> findQuestByMondoId(@Param("mondoId") Integer mondoId);

    /** Come {@link #findQuestByMondoId}, filtrata per stato di archiviazione: vedi la nota su {@link #findQuestByPersonaggioIdArchiviata}. */
    @Query("""
            SELECT i FROM Item i
            WHERE i.tipo = it.fin8.gdrsheet.def.TipoItem.QUEST
              AND i.personaggio IS NULL
              AND i.mondo.id = :mondoId
              AND NOT EXISTS (SELECT 1 FROM ItemLabel il2 WHERE il2.item = i AND il2.label = 'QUEST_PARTY')
              AND NOT EXISTS (
                  SELECT 1 FROM Collegamento c
                  WHERE c.itemTarget = i AND c.itemSource.tipo = it.fin8.gdrsheet.def.TipoItem.QUEST
              )
              AND (
                (:archiviate = true AND EXISTS (SELECT 1 FROM ItemLabel ila WHERE ila.item = i
                    AND ila.label = 'QUEST_ARCHIVIATA' AND ila.valore = '1'))
                OR (:archiviate = false AND NOT EXISTS (SELECT 1 FROM ItemLabel ila WHERE ila.item = i
                    AND ila.label = 'QUEST_ARCHIVIATA' AND ila.valore = '1'))
              )
            """)
    List<Item> findQuestByMondoIdArchiviata(@Param("mondoId") Integer mondoId, @Param("archiviate") boolean archiviate);

    /**
     * Quest "radice" (non ancora figlie di nessun'altra quest) che corrispondono al testo cercato:
     * candidate da collegare come sotto-quest di un'altra. Offrire solo radici è anche la garanzia
     * anti-ciclo: i discendenti di una quest non sono mai radici, quindi non possono comparire tra
     * i candidati e non si può creare un anello collegandone una.
     */
    @Query("""
            SELECT i FROM Item i
            WHERE i.tipo = it.fin8.gdrsheet.def.TipoItem.QUEST
              AND lower(i.nome) LIKE lower(concat('%', :q, '%'))
              AND NOT EXISTS (
                  SELECT 1 FROM Collegamento c
                  WHERE c.itemTarget = i AND c.itemSource.tipo = it.fin8.gdrsheet.def.TipoItem.QUEST
              )
            ORDER BY i.nome
            """)
    List<Item> searchQuestRadici(@Param("q") String q, org.springframework.data.domain.Pageable pageable);
}
