// Catalogo centrale delle label applicabili agli item e delle variabili
// utilizzabili nelle formule (modificatori e attacchi). Usato sia per popolare
// le select sia per i popup informativi.

export interface VoceInfo {
    nome: string
    descrizione: string
}

// Label generiche applicabili a un item dall'editor.
export const LABEL_CATALOG: VoceInfo[] = [
    {nome: 'PESO', descrizione: 'Peso dell’oggetto in kg. Concorre al peso totale trasportato (moltiplicato per QTA).'},
    {nome: 'QTA', descrizione: 'Quantità dell’oggetto. Moltiplica il peso e la variabile $QTA nelle formule dell’item.'},
    {nome: 'TAGLIA', descrizione: 'Imposta (SET) la taglia: sostituisce la taglia base nella taglia effettiva. Valori da -4 (Piccolissima) a 4 (Colossale).'},
    {nome: 'ADD_TAGLIA', descrizione: 'Aumenta/diminuisce la taglia effettiva (es. 1 = +1 categoria, -1 = -1). Si somma dopo l’eventuale SET.'},
    {nome: 'VISIBILITA', descrizione: 'Chi può vedere l’item: vuoto = tutti, OWNER = proprietario+master, MASTER = solo master.'},
    {nome: 'TIPO', descrizione: 'Tipo specifico dell’item (es. BARRIERA per i talenti che danno PF temporanei).'},
    {nome: 'BARR_MAX', descrizione: 'Talento barriera: PF massimi della barriera.'},
    {nome: 'BARR_CONS', descrizione: 'Talento barriera: PF consumati della barriera.'},
    {nome: 'DV', descrizione: 'Dadi vita concessi (es. da una classe).'},
    {nome: 'SPELL', descrizione: 'Lista incantesimi associata (id lista). Prefisso “+” per liste aggiuntive.'},
    {nome: 'COMP', descrizione: 'Competenze concesse dall’item.'},
    {nome: 'LINGUE', descrizione: 'Lingue concesse dall’item.'},
    {nome: 'REQ_COMP', descrizione: 'Competenza richiesta per usare l’item.'},
    {nome: 'MLDZN', descrizione: 'Marca il livello/oggetto come derivante da una maledizione.'},
    {nome: 'GRP_TRASF', descrizione: 'Gruppo di trasformazione (per la mutua esclusione tra trasformazioni/forme).'},
    {nome: 'ADD_CLASSE_<n>', descrizione: 'Aggiunge livelli alla N-esima classe: il valore è l’id della classe da aggiungere. <n> distingue più classi sullo stesso item.'},
    {nome: 'ADD_CLASSE_<n>_VALUE', descrizione: 'Numero di livelli da aggiungere alla N-esima classe (abbinata a ADD_CLASSE_<n>).'},
]

// Variabili utilizzabili nelle formule di modificatori e attacchi.
export const VARIABILI_FORMULA: VoceInfo[] = [
    {nome: 'FOR / DES / COS / INT / SAG / CAR', descrizione: 'Modificatore della caratteristica.'},
    {nome: 'TMP / RFL / VLT', descrizione: 'Modificatore del tiro salvezza (Tempra / Riflessi / Volontà).'},
    {nome: 'BAB', descrizione: 'Bonus di attacco base.'},
    {nome: 'LTT', descrizione: 'Bonus di lotta.'},
    {nome: 'MSC', descrizione: 'Bonus di attacco in mischia.'},
    {nome: 'GTT', descrizione: 'Bonus di attacco a distanza.'},
    {nome: 'CA / CAC / CAS', descrizione: 'Classe armatura / contatto / da sorpreso.'},
    {nome: 'LVL', descrizione: 'Livello totale del personaggio.'},
    {nome: 'PESO', descrizione: 'Peso del personaggio (label PESO), arrotondato.'},
    {nome: 'PESO_TOTALE', descrizione: 'Peso totale trasportato (personaggio + oggetti + monete), arrotondato.'},
    {nome: 'ETA', descrizione: 'Età del personaggio (label ETA).'},
    {nome: 'ALTEZZA', descrizione: 'Altezza del personaggio (label ALTEZZA).'},
    {nome: '$QTA', descrizione: 'Quantità dell’item corrente (label QTA).'},
    {nome: '$V_<nome>', descrizione: 'Contatore proprio dell’item (es. $V_LVL per il livello di un frutto).'},
    {nome: 'ECCESSO() / DIFETTO() / TRONCATO()', descrizione: 'Arrotondamento per eccesso / difetto / troncamento. Es.: DIFETTO(LVL/2).'},
]
