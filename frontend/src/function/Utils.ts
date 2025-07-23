export function getModificatoriFromItem(item, visited = new Set()) {
    if (!item || visited.has(item.id)) return [];

    visited.add(item.id);

    let result = [];

    // Aggiungi i modificatori dell'item corrente
    if (Array.isArray(item.modificatori)) {
        result.push(...item.modificatori);
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

    for (const itm of personaggio.abilita || []) {
        result.push(...getModificatoriFromItem(itm));
    }
    for (const itm of personaggio.razze || []) {
        result.push(...getModificatoriFromItem(itm));
    }

    return result;
}

