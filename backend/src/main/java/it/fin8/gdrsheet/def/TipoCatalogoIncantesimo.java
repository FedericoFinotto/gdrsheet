package it.fin8.gdrsheet.def;

/**
 * Le 4 liste "di corredo" di un incantesimo, ciascuna un catalogo di valori globale e condiviso
 * (come {@link it.fin8.gdrsheet.entity.ListaIncantesimi}) con abilitazione opt-in per mondo (vedi
 * {@link it.fin8.gdrsheet.entity.MondoCatalogoIncantesimoAbilitato}). A differenza delle liste/
 * domìni (codici SP_* con etichetta separata), qui il valore stesso è già l'etichetta mostrata
 * (es. "Abiurazione", "V") — non serve un codice distinto.
 */
public enum TipoCatalogoIncantesimo {
    SCUOLA, SOTTOSCUOLA, DESCRITTORE, COMPONENTE
}
