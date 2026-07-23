// Colori dei chip "In carico": assegnati in ordine di primo utilizzo, non per hash del testo —
// un hash-mod su una palette piccola può far collidere due testi diversi sullo stesso colore
// (è successo). Con un registro condiviso, ogni testo NUOVO prende il prossimo colore libero in
// palette, quindi due testi diversi non collidono mai finché restano colori liberi; lo stesso
// testo, già visto, riprende sempre lo stesso colore.
//
// Il registro deve vivere in un modulo a sé (non dentro un <script setup> di un .vue): il setup()
// di un componente viene rieseguito a ogni istanza — con QuestNode ricorsivo, ogni nodo avrebbe
// un proprio registro isolato invece di condividerlo con l'intero albero. Un modulo .ts viene
// invece valutato una sola volta e condiviso da tutti gli importatori.
const PALETTE = [
    {background: '#e0e7ff', color: '#3730a3'},
    {background: '#fce7f3', color: '#9d174d'},
    {background: '#dcfce7', color: '#166534'},
    {background: '#fef3c7', color: '#92400e'},
    {background: '#cffafe', color: '#155e75'},
    {background: '#fee2e2', color: '#991b1b'},
    {background: '#ede9fe', color: '#5b21b6'},
    {background: '#dbeafe', color: '#1d4ed8'},
]

const registro = new Map<string, { background: string; color: string }>()
let prossimo = 0

export function coloreIncarico(testo: string): { background: string; color: string } {
    let colore = registro.get(testo)
    if (!colore) {
        colore = PALETTE[prossimo % PALETTE.length]
        prossimo++
        registro.set(testo, colore)
    }
    return colore
}
