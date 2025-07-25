export function getModificatoriFromItem(item, visited = new Set()) {
    if (!item || visited.has(item.id)) return [];

    visited.add(item.id);

    let result = [];

    // Aggiungi i modificatori dell'item corrente
    if (Array.isArray(item.modificatori)) {
        result.push(...item.modificatori.map(itm => ({
            ...itm,
            tipo: item.tipo,
            nome: item.nome
        })));
    }

    // Visita i figli (ricorsivamente)
    if (Array.isArray(item.child)) {
        for (const link of item.child) {
            const childItem = link.idItemTarget;
            result.push(...getModificatoriFromItem(childItem, visited));
        }
    }

    return result;
}

export function getModificatoriFromPersonaggio(personaggio) {
    if (!personaggio) return [];
    let result = [];

    for (const itm of personaggio.items || []) {
        result.push(...getModificatoriFromItem(itm));
    }

    return result;
}


export function getDatiCaratteristica(personaggio, caratteristica, modsPersonaggio = undefined) {
    const stat = personaggio.character.stats?.find(s => s.stat?.id === caratteristica);

    if (modsPersonaggio === undefined) {
        modsPersonaggio = getModificatoriFromPersonaggio(personaggio.character);
    }
    const mods = modsPersonaggio.filter(mod => mod.stat.id === caratteristica);
    const bonus = mods.reduce((sum, mod) => sum + parseInt(mod.valore), 0);

    let statisticaBase = undefined;
    if (stat.mod !== null) {
        statisticaBase = getDatiCaratteristica(personaggio, stat.mod.id, modsPersonaggio);
    }

    if (stat.stat.tipo === 'CAR') {
        const valoreBase = parseInt(stat?.valore ?? 10);
        const valore = valoreBase + bonus;
        const modificatore = Math.floor((valore - 10) / 2);
        return {
            id: stat.stat.id,
            label: stat.stat.label,
            valoreBase: valoreBase,
            modificatori: mods,
            modificatore: modificatore,
            valore: valore
        }
    }
    if (stat.stat.tipo === 'TS') {
        const modificatore = statisticaBase?.modificatore + bonus;
        return {
            modificatore: modificatore,
            statisticaBase: statisticaBase,
            modificatori: mods
        }
    }

}


