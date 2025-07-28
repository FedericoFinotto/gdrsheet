package it.fin8.grdsheet.spellparser;

public class Incantesimo {
    public String nome;
    public String scuola;
    public String livello;
    public String componenti;
    public String tempo;
    public String range;
    public String effetto;
    public String durata;
    public String tiroSalvezza;
    public String resistenza;

    @Override
    public String toString() {
        return """
                Nome: %s
                Scuola: %s
                Livello: %s
                Componenti: %s
                Tempo: %s
                Range: %s
                Effetto: %s
                Durata: %s
                Tiro Salvezza: %s
                Resistenza: %s
                """.formatted(nome, scuola, livello, componenti, tempo, range, effetto, durata, tiroSalvezza, resistenza);
    }
}
