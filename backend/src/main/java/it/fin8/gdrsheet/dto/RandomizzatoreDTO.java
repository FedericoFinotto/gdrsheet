package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO del Randomizzatore: configurazione, richiesta di estrazione ed esito.
 */
public class RandomizzatoreDTO {

    /** Riferimento leggero a un item (categoria, tag o oggetto estratto). */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class RefDTO {
        private Integer id;
        private String nome;
    }

    /** Una categoria con i suoi tag selezionabili. */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class CategoriaDTO {
        private Integer id;
        private String nome;
        private List<RefDTO> tag;
    }

    /** Configurazione del randomizzatore, letta all'apertura della schermata di estrazione. */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ConfigDTO {
        private Integer id;
        private String nome;
        /** Categorie di cui l'utente deve scegliere un tag al momento dell'estrazione. */
        private List<CategoriaDTO> categorieScelte;
        /** Categorie di cui l'oggetto deve avere almeno un tag per essere candidato. */
        private List<CategoriaDTO> categoriePresenti;
        /** PRODOTTO | SOMMA */
        private String combina;
    }

    /** Richiesta di estrazione: per ogni categoria scelta, il tag selezionato dall'utente. */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class EstrazioneRequest {
        /** Id dei tag scelti, uno per ogni categoria scelta. */
        private List<Integer> tagScelti;
        /** Contesto di visibilità: mondo e sistema correnti. */
        private Integer idMondo;
        private Integer idSistema;
    }

    /** Un tag estratto per una delle categorie "presenti". */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class TagEstrattoDTO {
        private Integer idCategoria;
        private String categoria;
        private Integer idTag;
        private String tag;
        /** True se il tag non è stato scelto né ereditato, ma sorteggiato d'ufficio. */
        private Boolean automatico;

        public TagEstrattoDTO(Integer idCategoria, String categoria, Integer idTag, String tag) {
            this(idCategoria, categoria, idTag, tag, null);
        }
    }

    /**
     * Esito di un'estrazione. È un albero: un oggetto estratto può innescare altri
     * randomizzatori (label RAND_TRIGGER), i cui esiti finiscono in {@link #figli}.
     */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class EsitoDTO {
        private Integer idItem;
        private String nome;
        private String descrizione;
        /** Tag scelti dall'utente (o ereditati dal livello superiore). */
        private List<TagEstrattoDTO> scelti;
        /** Un tag estratto per ognuna delle categorie richieste come presenti. */
        private List<TagEstrattoDTO> estratti;
        /** Quanti oggetti erano in gara. */
        private int candidati;
        private LocalDateTime eseguitoIl;
        /** Randomizzatore che ha prodotto questo esito (utile nei livelli annidati). */
        private Integer idRandomizzatore;
        private String randomizzatore;
        /** Esiti innescati da questo oggetto. */
        private List<EsitoDTO> figli;
        /** Valorizzato quando un ramo non è stato risolto (nessun candidato, limiti raggiunti…). */
        private String avviso;

        public EsitoDTO(Integer idItem, String nome, String descrizione,
                        List<TagEstrattoDTO> scelti, List<TagEstrattoDTO> estratti,
                        int candidati, LocalDateTime eseguitoIl) {
            this(idItem, nome, descrizione, scelti, estratti, candidati, eseguitoIl,
                    null, null, null, null);
        }
    }

    /** Voce di storico. */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class StoricoDTO {
        private Integer id;
        private LocalDateTime eseguitoIl;
        private String utente;
        private EsitoDTO esito;
    }
}
