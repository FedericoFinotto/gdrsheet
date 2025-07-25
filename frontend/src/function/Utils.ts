export function getModificatoriFromItem(item, visited = new Set()) {
    if (!item || visited.has(item.id)) return [];

    visited.add(item.id);

    let result = [];

    // Aggiungi i modificatori dell'item corrente
    if (Array.isArray(item.modificatori)) {
        result.push(...item.modificatori.map(itm => ({
            ...itm,
            tipo: item.tipo, // Aggiunto come chiave: valore
            nome: item.nome  // Aggiunto come chiave: valore
        })));
    }

    // Visita i figli (ricorsivamente)
    if (Array.isArray(item.child)) {
        for (const link of item.child) {
            const childItem = link.idItemTarget; // Assicurati che childItem sia l'oggetto completo, non solo l'ID
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
    for (const itm of personaggio.altri || []) {
        result.push(...getModificatoriFromItem(itm));
    }
    for (const itm of personaggio.armi || []) {
        result.push(...getModificatoriFromItem(itm));
    }
    for (const itm of personaggio.consumabili || []) {
        result.push(...getModificatoriFromItem(itm));
    }
    for (const itm of personaggio.equipaggiamento || []) {
        result.push(...getModificatoriFromItem(itm));
    }
    for (const itm of personaggio.munizioni || []) {
        result.push(...getModificatoriFromItem(itm));
    }
    for (const itm of personaggio.oggetti || []) {
        result.push(...getModificatoriFromItem(itm));
    }
    for (const itm of personaggio.talenti || []) {
        result.push(...getModificatoriFromItem(itm));
    }

    return result;
}

