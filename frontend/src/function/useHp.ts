import {computed} from 'vue'
import {storeToRefs} from 'pinia'
import debounce from 'lodash/debounce'
import {updateBarriera, updateHP} from '../service/PersonaggioService'
import {useCharacterStore} from '../stores/personaggio'

export interface Barriera {
  id: number
  nome: string
  max: number
  cons: number
  current: number
}

function clamp(min: number, v: number, max: number) {
  return Math.min(max, Math.max(min, v))
}

/**
 * Stato e azioni su HP, HP temporanei e barriere di un personaggio.
 * Lavora direttamente sulla cache reattiva (aggiornamento ottimistico) e
 * sincronizza il backend in debounce.
 */
export function useHp(idPersonaggio: number) {
  const {cache} = storeToRefs(useCharacterStore())

  const pfStat = computed(() =>
      cache.value[idPersonaggio]?.modificatori?.contatori?.find((s: any) => s.id === 'PF'))
  const pfTempStat = computed(() =>
      cache.value[idPersonaggio]?.modificatori?.contatori?.find((s: any) => s.id === 'PFTEMP'))

  const hpMax = computed<number>(() => pfStat.value?.max ?? 0)
  const damageNeg = computed<number>(() => pfStat.value?.valore ?? 0)  // <= 0
  const hp = computed<number>(() => Math.max(0, hpMax.value + damageNeg.value))
  const pfTemp = computed<number>(() => pfTempStat.value?.valore ?? 0)

  // --- barriere: talenti con label TIPO=BARRIERA ---
  const barriere = computed<Barriera[]>(() => {
    const talenti = cache.value[idPersonaggio]?.items?.talenti ?? []
    return talenti
        .filter((t: any) => t.barriera)
        .map((t: any) => {
          const max = Number(t.barrMax) || 0
          const cons = Number(t.barrCons) || 0
          return {id: t.id, nome: t.nome, max, cons, current: Math.max(0, max - cons)}
        })
        .filter((b: Barriera) => b.max > 0)
  })

  const barriereTotal = computed<number>(() =>
      barriere.value.reduce((s, b) => s + b.current, 0))
  // una barriera consumata del tutto (current 0) esce dal totale: la barra si
  // ribilancia. Una barriera parzialmente consumata resta col suo grigio.
  const barriereMaxTotal = computed<number>(() =>
      barriere.value.reduce((s, b) => s + (b.current > 0 ? b.max : 0), 0))

  // capacità totale (denominatore) e vita totale rimanente (numeratore)
  const totalMax = computed<number>(() => hpMax.value + pfTemp.value + barriereMaxTotal.value)
  const remaining = computed<number>(() => hp.value + pfTemp.value + barriereTotal.value)

  function talentiBarriera(): any[] {
    return (cache.value[idPersonaggio]?.items?.talenti ?? []).filter((t: any) => t.barriera)
  }

  // --- sync backend ---
  const debouncedHp = debounce(() => {
    updateHP(idPersonaggio, pfStat.value?.valore ?? 0, pfTempStat.value?.valore ?? 0)
        .catch(e => console.error('Errore sync HP:', e))
  }, 1200)

  function syncBarriera(id: number, cons: number) {
    updateBarriera(id, cons, idPersonaggio).catch(e => console.error('Errore sync barriera:', e))
  }

  /**
   * Danno (amount<0): consuma in ordine barriere (scudi) → PF temp → vita.
   * Cura (amount>0): riduce i danni verso 0 (vita; non ripristina temp/barriere).
   */
  function modifyHp(amount: number) {
    if (!pfStat.value) return

    if (amount < 0) {
      let dmg = -amount

      // 1) barriere (scudi), nell'ordine in cui compaiono
      for (const t of talentiBarriera()) {
        if (dmg <= 0) break
        const max = Number(t.barrMax) || 0
        const cons = Number(t.barrCons) || 0
        const current = max - cons
        if (current <= 0) continue
        const cut = Math.min(dmg, current)
        t.barrCons = cons + cut
        syncBarriera(t.id, t.barrCons)
        dmg -= cut
      }

      // 2) PF temporanei
      const curTemp = pfTempStat.value?.valore ?? 0
      if (dmg > 0 && pfTempStat.value && curTemp > 0) {
        const cut = Math.min(dmg, curTemp)
        pfTempStat.value.valore = curTemp - cut
        dmg -= cut
      }

      // 3) vita reale
      if (dmg > 0) {
        const curDamage = pfStat.value.valore ?? 0
        pfStat.value.valore = clamp(-hpMax.value, curDamage - dmg, 0)
      }
    } else if (amount > 0) {
      const curDamage = pfStat.value.valore ?? 0
      const curable = -curDamage
      if (curable > 0) pfStat.value.valore = curDamage + Math.min(amount, curable)
    }
    debouncedHp()
  }

  /** Modifica diretta dei PF temporanei (anche negativa, non sotto 0). */
  function modifyTemp(amount: number) {
    if (!pfTempStat.value) return
    pfTempStat.value.valore = Math.max(0, (pfTempStat.value.valore ?? 0) + amount)
    debouncedHp()
  }

  function setTemp(value: number) {
    if (!pfTempStat.value) return
    pfTempStat.value.valore = Math.max(0, Math.floor(value) || 0)
    debouncedHp()
  }

  function getTalento(id: number): any | undefined {
    return (cache.value[idPersonaggio]?.items?.talenti ?? []).find((t: any) => t.id === id)
  }

  /** delta>0 ripristina la barriera, delta<0 la consuma. */
  function modifyBarriera(id: number, delta: number) {
    const t = getTalento(id)
    if (!t) return
    const max = Number(t.barrMax) || 0
    const cons = clamp(0, (Number(t.barrCons) || 0) - delta, max)
    t.barrCons = cons
    syncBarriera(id, cons)
  }

  function resetBarriera(id: number) {
    const t = getTalento(id)
    if (!t) return
    t.barrCons = 0
    syncBarriera(id, 0)
  }

  function distruggiBarriera(id: number) {
    const t = getTalento(id)
    if (!t) return
    const max = Number(t.barrMax) || 0
    t.barrCons = max
    syncBarriera(id, max)
  }

  return {
    hp, hpMax, pfTemp, barriere, barriereTotal, barriereMaxTotal,
    totalMax, remaining,
    modifyHp, modifyTemp, setTemp,
    modifyBarriera, resetBarriera, distruggiBarriera,
  }
}
