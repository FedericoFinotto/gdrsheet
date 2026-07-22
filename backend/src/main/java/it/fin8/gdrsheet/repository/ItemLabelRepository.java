package it.fin8.gdrsheet.repository;

import it.fin8.gdrsheet.entity.ItemLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemLabelRepository extends JpaRepository<ItemLabel, Integer> {
    public List<ItemLabel> findByItemPersonaggioIdAndItemNome(
            Integer personaggioId,
            String itemName
    );

    /**
     * Restituisce tutti gli ItemLabel che hanno esattamente quella label
     * e il cui item.id è contenuto nella lista itemIds.
     */
    List<ItemLabel> findByLabelAndItem_IdIn(String label, List<Integer> itemIds);

    List<ItemLabel> findByLabelLikeAndItem_IdIn(String label, List<Integer> itemIds);

    // Solo label globali (id_personaggio IS NULL)
    List<ItemLabel> findByLabelAndItem_IdInAndPersonaggioIsNull(String label, List<Integer> itemIds);

    // Label per-personaggio
    List<ItemLabel> findByLabelAndItem_IdInAndPersonaggio_Id(String label, List<Integer> itemIds, Integer personaggioId);

    java.util.Optional<ItemLabel> findByItem_IdAndLabelAndPersonaggio_Id(Integer itemId, String label, Integer personaggioId);

    void deleteByLabelAndPersonaggio_Id(String label, Integer personaggioId);

    /**
     * Triple (itemId, label, valore) per le label richieste, sugli item con id nella lista data
     * (es. per calcolaPeso, a partire dal flatten già calcolato da PersonaggioService). Non filtra
     * per id_personaggio: le label globali (compendio) e quelle per-personaggio (es. QTA) sulla
     * stessa chiave convivono nel risultato.
     */
    @Query("""
            SELECT il.item.id, il.label, il.valore FROM ItemLabel il
            WHERE il.label IN :labels AND il.item.id IN :itemIds
            """)
    List<Object[]> findLabelValuesByItemIds(
            @Param("labels") java.util.Collection<String> labels,
            @Param("itemIds") java.util.Collection<Integer> itemIds
    );

    /**
     * Id dei personaggi che hanno un LIVELLO la cui label CLASSE punta a questa classe/razza —
     * l'associazione LIVELLO→CLASSE passa per una label, non per un Collegamento, quindi non è
     * raggiungibile dalla risalita del grafo usata per gli altri item (vedi
     * CollegamentoRepository#findPersonaggiRaggiungibiliDaItem). Usata per invalidare la cache di
     * tutti i personaggi che usano una classe/razza quando questa viene modificata.
     */
    @Query("""
            SELECT DISTINCT il.item.personaggio.id FROM ItemLabel il
            WHERE il.label = 'CLASSE' AND il.valore = :classeId
              AND il.item.personaggio.id IS NOT NULL
            """)
    List<Integer> findPersonaggiByClasseLabel(@Param("classeId") String classeId);
}
