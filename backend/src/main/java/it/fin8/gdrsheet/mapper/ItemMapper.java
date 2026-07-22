package it.fin8.gdrsheet.mapper;

import it.fin8.gdrsheet.config.Constants;
import it.fin8.gdrsheet.dto.*;
import it.fin8.gdrsheet.entity.Collegamento;
import it.fin8.gdrsheet.entity.Item;
import it.fin8.gdrsheet.repository.ItemRepository;
import it.fin8.gdrsheet.service.CalcoloService;
import it.fin8.gdrsheet.service.UtilService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ItemMapper {

    private final UtilService utilService;
    private final ItemRepository itemRepository;
    private final CalcoloService calcoloService;

    public ItemMapper(UtilService utilService, ItemRepository itemRepository, CalcoloService calcoloService) {
        this.utilService = utilService;
        this.itemRepository = itemRepository;
        this.calcoloService = calcoloService;
    }

    public ItemDTO toDTO(Item entity) {
        return toDTO(entity, null, null);
    }

    public ItemDTO toDTO(Item entity, Integer utilizziTotale, Integer utilizziUsati) {
        ItemDTO dto = new ItemDTO();
        populateBase(dto, entity, utilizziTotale, utilizziUsati);
        return dto;
    }

    /** FRUTTO: stessi campi di {@link #toDTO}, più il campo trasformazioni (valorizzato dal chiamante). */
    public FruttoDTO toFruttoDTO(Item entity, Integer utilizziTotale, Integer utilizziUsati) {
        FruttoDTO dto = new FruttoDTO();
        populateBase(dto, entity, utilizziTotale, utilizziUsati);
        dto.setTrasformazioni(new ArrayList<>());
        return dto;
    }

    private void populateBase(ItemDTO dto, Item entity, Integer utilizziTotale, Integer utilizziUsati) {
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setTipo(entity.getTipo());
        dto.setDisabled(isDisabled(entity));
        dto.setQuantita(parseQuantita(entity.getLabel(Constants.LABEL_QTA)));
        dto.setManuale(entity.getLabel(Constants.ITEM_LABEL_MANUALE));

        if (Constants.ITEM_TIPO_BARRIERA.equalsIgnoreCase(entity.getLabel(Constants.ITEM_LABEL_TIPO))) {
            dto.setBarriera(true);
            dto.setBarrMax(parseIntOrZero(entity.getLabel(Constants.ITEM_LABEL_BARR_MAX)));
            dto.setBarrCons(parseIntOrZero(entity.getLabel(Constants.ITEM_LABEL_BARR_CONS)));
        }

        // Utilizzi: totale dall'item (globale), usati per-personaggio
        Integer tot = utilizziTotale != null ? utilizziTotale
                : parseIntOrNull(entity.getLabel(Constants.LABEL_UTILIZZI));
        if (tot != null) {
            dto.setUtilizziTotale(tot);
            dto.setUtilizziUsati(utilizziUsati != null ? utilizziUsati : 0);
        }
        // Peso, capienza e flag armi (solo su CONTENITORE)
        String pesoStr = entity.getLabel(Constants.LABEL_PESO);
        if (pesoStr != null) dto.setPeso(parseDouble(pesoStr));
        String capienzaStr = entity.getLabel(Constants.LABEL_CAPIENZA);
        if (capienzaStr != null) dto.setCapienza(parseDouble(capienzaStr));
        if ("1".equals(entity.getLabel(Constants.LABEL_INCLUDI_ARMI_ABILITATE)))
            dto.setIncludiArmiAbilitate(true);
        if ("1".equals(entity.getLabel(Constants.LABEL_INCLUDI_OGGETTI_ABILITATI)))
            dto.setIncludiOggettiAbilitati(true);
        if ("1".equals(entity.getLabel(Constants.LABEL_INCLUDI_CONSUMABILI_ABILITATI)))
            dto.setIncludiConsumabiliAbilitati(true);
        if ("1".equals(entity.getLabel(Constants.LABEL_INCLUDI_TUTTI_ABILITATI)))
            dto.setIncludiTuttiAbilitati(true);

        if ("1".equals(entity.getLabel(Constants.ITEM_LABEL_DESCR_STRAORDINARIA)))
            dto.setDescrStraordinaria(true);
        if ("1".equals(entity.getLabel(Constants.ITEM_LABEL_DESCR_MAGICA)))
            dto.setDescrMagica(true);
        if ("1".equals(entity.getLabel(Constants.ITEM_LABEL_DESCR_SOPRANNATURALE)))
            dto.setDescrSoprannaturale(true);
        if ("1".equals(entity.getLabel(Constants.ITEM_LABEL_DESCR_NATURALE)))
            dto.setDescrNaturale(true);
        if ("1".equals(entity.getLabel(Constants.ITEM_LABEL_DESCR_DIVINA)))
            dto.setDescrDivina(true);

        if ("1".equals(entity.getLabel(Constants.ITEM_LABEL_MAGICO)))
            dto.setMagico(true);
        if ("1".equals(entity.getLabel(Constants.ITEM_LABEL_PSIONICO)))
            dto.setPsionico(true);
        if ("1".equals(entity.getLabel(Constants.ITEM_LABEL_DIVINO)))
            dto.setDivino(true);
        if ("1".equals(entity.getLabel(Constants.ITEM_LABEL_LEGGENDARIO)))
            dto.setLeggendario(true);
        if ("1".equals(entity.getLabel(Constants.ITEM_LABEL_UNICO)))
            dto.setUnico(true);

        // Prefisso ereditato da un item genitore (label PREFISSO_OGGETTI): mostrato come chip
        // prima del nome nell'inventario sui suoi item collegati (child).
        if (entity.getParent() != null) {
            for (Collegamento p : entity.getParent()) {
                Item source = p.getItemSource();
                String prefisso = source != null ? source.getLabel(Constants.ITEM_LABEL_PREFISSO_OGGETTI) : null;
                if (prefisso != null && !prefisso.isBlank()) {
                    dto.setPrefissoOggetti(prefisso.trim());
                    break;
                }
            }
        }
    }

    private static Integer parseQuantita(String s) {
        if (s == null) return 1;
        try {
            return Math.max(0, Integer.parseInt(s.trim()));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private static Double parseDouble(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Double.parseDouble(s.trim().replace(',', '.')); } catch (NumberFormatException e) { return null; }
    }

    private static Integer parseIntOrZero(String s) {
        if (s == null) return 0;
        try {
            return Math.max(0, Integer.parseInt(s.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public LivelloDTO toLivelloDTO(Item entity) {
        LivelloDTO dto = new LivelloDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setTipo(entity.getTipo());
        dto.setDisabled(isDisabled(entity));
        dto.setLivello(Integer.parseInt(entity.getLabel(Constants.ITEM_LIVELLO_LVL)));
        String classeString = entity.getLabel(Constants.ITEM_LABEL_CLASSE);
        String maledizioneString = entity.getLabel(Constants.ITEM_LABEL_MALEDIZIONE);

        Item classeItem = null;
        if (classeString != null) {
            Integer classeId = Integer.valueOf(classeString);
            classeItem = itemRepository.findItemById(classeId);
            dto.setClasse(classeItem.getNome());
            dto.setClasseId(classeId);
        }
        if (maledizioneString != null) {
            dto.setMaledizione(maledizioneString);
        }
        dto.setLivelliClasse(utilService.parseStringToIntList(entity.getLabel(Constants.ITEM_LIVELLO_LVL_CLASSE)));
        String gradiString = entity.getLabel(Constants.ITEM_LABEL_GRADI_LIVELLO);
        if (gradiString != null && !gradiString.isBlank()) {
            try {
                dto.setGradi(Integer.valueOf(gradiString.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (classeItem != null && dto.getGradi() != null) {
            int numLivelliClasse = dto.getLivelliClasse() != null && !dto.getLivelliClasse().isEmpty()
                    ? dto.getLivelliClasse().size() : 1;
            dto.setIntModEquivalente(risolviModIntPerGradi(classeItem, dto.getLivello(), numLivelliClasse, dto.getGradi()));
        }
        return dto;
    }

    /**
     * Reverse-solve ESATTO (non a tentativi): quale modificatore di Intelligenza, dato in pasto
     * alle formule RANK_1/RANK della classe, produce esattamente {@code gradiTarget}? Rispecchia
     * la logica di {@code PersonaggioService.computeGradi} (stesso branch primo-livello vs altri,
     * stesso moltiplicatore per il numero di livelli-classe combinati in questo Livello).
     * <p>
     * Il "totale gradi" in funzione del modificatore INT è quasi sempre LINEARE (la stragrande
     * maggioranza delle formule è "@INT + N" o "k*(@INT + N)"): valutando la formula in due punti
     * qualsiasi (0 e 1) si ricavano coefficiente angolare e intercetta, e si risolve l'equazione
     * algebricamente — nessun limite/range di ricerca, funziona per qualunque modificatore, anche
     * enormi. Il risultato viene poi RIVERIFICATO valutando la formula col modificatore trovato:
     * se non torna esatto (formula non lineare, es. arrotondamenti asimmetrici o dadi), niente
     * scorciatoie sbagliate — si prova comunque una scansione ad ampio raggio come ultima risorsa.
     */
    private Integer risolviModIntPerGradi(Item classe, Integer livello, int numLivelliClasse, int gradiTarget) {
        String fRank = classe.getLabel(Constants.ITEM_LABEL_RANK);
        String fRank1 = classe.getLabel(Constants.ITEM_LABEL_RANK_PRIMO);
        if ((fRank == null || fRank.isBlank()) && (fRank1 == null || fRank1.isBlank())) return null;

        boolean primoLivello = livello != null && livello == 1;
        int n = Math.max(1, numLivelliClasse);
        java.util.function.IntUnaryOperator totale = mod -> {
            List<CaratteristicaDTO> car = List.of(new CaratteristicaDTO("INT", "INT", null, mod, null, null));
            Integer rank = valutaFormulaOrNull(fRank, car);
            Integer rank1 = valutaFormulaOrNull(fRank1, car);
            if (primoLivello) {
                int primo = rank1 != null ? rank1 : (rank != null ? rank : 0);
                int altri = rank != null ? rank * (n - 1) : 0;
                return primo + altri;
            }
            return rank != null ? rank * n : Integer.MIN_VALUE;
        };

        // 1) risoluzione algebrica esatta (nessun limite): assume linearità, verificata dopo.
        int y0 = totale.applyAsInt(0);
        int y1 = totale.applyAsInt(1);
        if (y0 != Integer.MIN_VALUE && y1 != Integer.MIN_VALUE) {
            int slope = y1 - y0;
            if (slope != 0 && (gradiTarget - y0) % slope == 0) {
                int candidato = (gradiTarget - y0) / slope;
                if (totale.applyAsInt(candidato) == gradiTarget) return candidato;
            } else if (slope == 0 && y0 == gradiTarget) {
                return 0; // formula costante (non dipende da INT) e già combacia
            }
        }

        // 2) fallback per formule non lineari (arrotondamenti, dadi, ecc.): scansione ad ampio raggio.
        for (int mod = -50; mod <= 1000; mod++) {
            if (totale.applyAsInt(mod) == gradiTarget) return mod;
        }
        return null;
    }

    private Integer valutaFormulaOrNull(String formula, List<CaratteristicaDTO> caratteristiche) {
        if (formula == null || formula.isBlank()) return null;
        try {
            return Integer.parseInt(calcoloService.calcola(formula, caratteristiche));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public SpellBookIncantesimoDTO toIncantesimoDTO(Item classe, ItemLivelloDTO itemLivelloDTO) {
        Item entity = itemLivelloDTO.getItem();
        SpellBookIncantesimoDTO dto = new SpellBookIncantesimoDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setTipo(entity.getTipo());
        dto.setLivello(Integer.parseInt(itemLivelloDTO.getLivello()));
        dto.setIdClasse(classe.getId());
        dto.setClasse(classe.getNome());
        dto.setSpellList(utilService.getItemLabel(classe, Constants.ITEM_LABEL_LISTA_INCANTESIMI));
        dto.setComponenti(utilService.getItemLabels(entity, Constants.ITEM_LABEL_COMPONENTE));
        dto.setScuola(utilService.getItemLabel(entity, Constants.ITEM_LABEL_SCUOLA_SP));
        return dto;
    }

    public SpellBookIncantesimoDTO toIncantesimoDTO(Item classe, Collegamento coll) {
        Item entity = coll.getItemTarget();
        boolean alwaysPrep = Objects.equals(coll.getLabel(Constants.COLLEGAMENTO_LABEL_N_PREPARATI), Constants.COLLEGAMENTO_LABEL_ALWAYS_PREPARED);
        SpellBookIncantesimoDTO dto = new SpellBookIncantesimoDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setTipo(entity.getTipo());
        dto.setLivello(Integer.parseInt(utilService.getCollegamentoLabel(coll, Constants.COLLEGAMENTO_LABEL_LIVELLO)));
        dto.setIdClasse(classe.getId());
        dto.setClasse(classe.getNome());
        dto.setSpellList(utilService.getCollegamentoLabel(coll, Constants.COLLEGAMENTO_LABEL_LISTA_INCANTESIMI));
        dto.setComponenti(utilService.getItemLabels(coll.getItemTarget(), Constants.ITEM_LABEL_COMPONENTE));
        if (alwaysPrep) {
            dto.setAlwaysPrep(true);
            dto.setNPrepared(null);
            dto.setNUsed(null);
        } else {
            dto.setAlwaysPrep(false);
            dto.setNPrepared(Integer.parseInt(utilService.getCollegamentoLabel(coll, Constants.COLLEGAMENTO_LABEL_N_PREPARATI)));
            dto.setNUsed(Integer.parseInt(utilService.getCollegamentoLabel(coll, Constants.COLLEGAMENTO_LABEL_N_USATI)));
        }
        return dto;
    }

    public AttaccoDTO toAttaccoDTO(Item entity) {
        List<Collegamento> parents = entity.getParent();
        String nomeItemParent = null;
        if (parents != null && !parents.isEmpty()) {
            Item parent = parents.get(0).getItemSource();
            nomeItemParent = parent.getNome();
        }
        AttaccoDTO dto = new AttaccoDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setTipo(entity.getTipo());
        dto.setNomeItem(nomeItemParent);
        dto.setTipoRisoluzione(entity.getLabel(Constants.ITEM_LABEL_ATTACCO_TIPO_RISOLUZIONE));
        dto.setAttacco(entity.getLabel(Constants.ITEM_LABEL_ATTACCO_TIRO_PER_COLPIRE));
        dto.setTiroSalvezza(entity.getLabel(Constants.ITEM_LABEL_ATTACCO_TIRO_SALVEZZA));
        dto.setTiroSalvezzaCd(entity.getLabel(Constants.ITEM_LABEL_ATTACCO_TIRO_SALVEZZA_CD));
        dto.setRange(entity.getLabel(Constants.ITEM_LABEL_ATTACCO_RANGE));
        dto.setDanni(readDanni(entity));

        return dto;
    }

    private static final String DANNO_SEP = "␞";

    /** Righe ATK_DANNO ("formula␞tipo"), oppure — dati vecchi salvati col modello a un solo
     * danno per attacco — un'unica riga sintetizzata da TPD/TDANNO. */
    private List<AttaccoDTO.DannoDTO> readDanni(Item entity) {
        List<AttaccoDTO.DannoDTO> out = new ArrayList<>();
        if (entity.getLabels() != null) {
            for (var l : entity.getLabels()) {
                if (!Constants.ITEM_LABEL_ATTACCO_DANNO.equals(l.getLabel()) || l.getValore() == null) continue;
                String[] parts = l.getValore().split(DANNO_SEP, 2);
                String formula = parts.length > 0 ? parts[0] : "";
                String tipo = parts.length > 1 ? parts[1] : "";
                if (!formula.isBlank()) out.add(new AttaccoDTO.DannoDTO(formula, tipo));
            }
        }
        if (out.isEmpty()) {
            String legacyFormula = entity.getLabel(Constants.ITEM_LABEL_ATTACCO_DANNI);
            if (legacyFormula != null && !legacyFormula.isBlank()) {
                String legacyTipo = entity.getLabel(Constants.ITEM_LABEL_ATTACCO_TIPO_DANNI);
                out.add(new AttaccoDTO.DannoDTO(legacyFormula, legacyTipo == null ? "" : legacyTipo));
            }
        }
        return out;
    }

    public Boolean isDisabled(Item entity) {
        String disabledLabel = entity.getLabel(Constants.ITEM_LABEL_DISABILITATO);
        return disabledLabel != null && disabledLabel.equals("1");
    }

    public ClasseDTO toClasseDTO(InfoClasseDTO classe) {
        Item entity = classe.getClasse();
        ClasseDTO dto = new ClasseDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setTipo(entity.getTipo());
        dto.setLivelli(classe.getLivelli().stream().toList());
        dto.setSpell(utilService.getItemLabel(entity, Constants.ITEM_LABEL_LISTA_INCANTESIMI));
        return dto;
    }

    public Collegamento toNewCollegamentoIncantesimo(Map.Entry<Integer, Integer> sp, Integer sourceId, String livello, String spellList, EntityManager em) {
        Integer targetId = sp.getKey();
        Collegamento col = new Collegamento();
        col.setItemSource(em.getReference(Item.class, sourceId));
        col.setItemTarget(em.getReference(Item.class, targetId));
        col.setLabels(new ArrayList<>());
        col.setLabel(Constants.COLLEGAMENTO_LABEL_LIVELLO, livello);
        col.setLabel(Constants.COLLEGAMENTO_LABEL_LISTA_INCANTESIMI, spellList);
        if (sp.getValue() == -54) {
            col.setLabel(Constants.COLLEGAMENTO_LABEL_N_PREPARATI, "ALWAYS");
        } else {
            col.setLabel(Constants.COLLEGAMENTO_LABEL_N_PREPARATI, sp.getValue().toString());
            col.setLabel(Constants.COLLEGAMENTO_LABEL_N_USATI, "0");
        }
        return col;
    }

    public TrasformazioneDTO toTrasformazioneDTO(Item entity) {
        TrasformazioneDTO dto = new TrasformazioneDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setTipo(entity.getTipo());
        dto.setDisabled(isDisabled(entity));
        dto.setGruppo(entity.getLabel(Constants.ITEM_LABEL_GRUPPO_TRASFORMAZIONE));
        return dto;
    }
}
