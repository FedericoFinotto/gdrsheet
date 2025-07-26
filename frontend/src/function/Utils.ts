export function getModificatoriFromItem(item, visited = new Set()) {
    if (!item || visited.has(item.id)) return [];

    visited.add(item.id);

    let result = [];

    if (Array.isArray(item.modificatori)) {
        let abilitaDiClasse: string[] = [];

        if (item.tipo === 'LIVELLO') {
            const classe = item.child.find(i => i.itemTarget?.tipo === 'CLASSE')?.itemTarget;
            const labelAbClasse = classe?.labels?.find(x => x.label === 'ABCLASSE');
            const abilitaDiClasseStringa: string = labelAbClasse?.valore ?? '';
            abilitaDiClasse = abilitaDiClasseStringa ? abilitaDiClasseStringa.split(',') : [];
        }

        result.push(...item.modificatori.map(itm => ({
            ...itm,
            item_tipo: item.tipo,
            item_nome: item.nome,
            item_ab_classe: abilitaDiClasse,
        })));
    }

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
    if (stat.stat.tipo === 'AB') {
        const modificatoriVALORE = mods.filter(mod => mod.tipo === 'VALORE');
        const modificatoriRANK = mods.filter(mod => mod.tipo === 'RANK');

        let abClasse = false;
        if (modificatoriRANK.length > 0) {
            abClasse = modificatoriRANK[0].item_ab_classe.includes(stat.stat.id);
        }

        if (modificatoriVALORE.length > 0) {
            console.log('MADONNA BLASFEMA', modificatoriVALORE);
        }

        const test = stat.stat.label;
        const bonusVALORE = modificatoriVALORE.filter(m => m.sempreAttivo).reduce((sum, mod) => sum + parseInt(mod.valore), 0);
        const rank = modificatoriRANK.reduce((sum, mod) => sum + parseInt(mod.valore), 0);
        const bonusRank = (abClasse || stat.classe) ? rank : Math.floor(rank / 2);
        const modificatore = (statisticaBase?.modificatore ?? 0) + bonusVALORE + bonusRank;

        return {
            rank: {
                valore: rank,
                modificatore: bonusRank,
                modificatori: modificatoriRANK,
                addestramento: stat.addestramento,
                classe: stat.classe,
            },
            statistica: {
                id: stat.stat.id,
                label: stat.stat.label,
                modificatore: modificatore,
                modificatori: modificatoriVALORE,
            },
            base: statisticaBase ?? null,
        }
    }

}


export function testoModificatore(mod: number) {
    if (mod >= 0) return "+" + mod;
    return mod;
}


