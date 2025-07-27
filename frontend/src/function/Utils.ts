import {TIPO_ITEM} from "./Constants";

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

    // if (Array.isArray(item.child)) {
    //     for (const link of item.child) {
    //         const childItem = link.itemTarget;
    //         result.push(...getModificatoriFromItem(childItem, visited));
    //     }
    // }

    return result;
}

export function getModificatoriFromPersonaggio(personaggio) {
    if (!personaggio) return [];
    let result = [];

    for (const itm of getAllItems(personaggio) || []) {
        result.push(...getModificatoriFromItem(itm));
    }

    return result;
}


export function getDatiCaratteristica(personaggio, caratteristica, modsPersonaggio = undefined) {
    const stat = personaggio.character.stats?.find(s => s.stat?.id === caratteristica);

    let carToFind = caratteristica;
    if (caratteristica === 'CAS' || caratteristica === 'CAC') {
        carToFind = 'CA';
    }
    if (modsPersonaggio === undefined) {
        modsPersonaggio = getModificatoriFromPersonaggio(personaggio.character);
    }
    const mods = modsPersonaggio.filter(mod => mod.stat.id === carToFind);
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
    if (stat.stat.tipo === 'CA') {
        const prendiMassimo = (mods: any[]) =>
            mods.length > 0
                ? Math.max(...mods.map(mod => parseInt(mod.valore)))
                : 0;

        const modificatoriSchivare = mods.filter(mod => mod.tipo === 'CA_SCHIVARE');
        const modificatoriArmatura = mods.filter(mod => mod.tipo === 'CA_ARMOR');
        const modificatoriNaturale = mods.filter(mod => mod.tipo === 'CA_NATURALE');
        const modificatoriDeviazione = mods.filter(mod => mod.tipo === 'CA_DEVIAZIONE');
        const modificatoriScudo = mods.filter(mod => mod.tipo === 'CA_SHIELD');
        const modificatoriMagici = mods.filter(mod => mod.tipo === 'CA_MAGIC');
        const modificatoriValore = mods.filter(mod => mod.tipo === 'VALORE');


        const bonusVALORE = modificatoriValore.filter(m => m.sempreAttivo).reduce((sum, mod) => sum + parseInt(mod.valore), 0);
        const modificatoreSchivare = prendiMassimo(modificatoriSchivare);
        const modificatoreArmatura = prendiMassimo(modificatoriArmatura);
        const modificatoreNaturale = prendiMassimo(modificatoriNaturale);
        const modificatoreDeviazione = prendiMassimo(modificatoriDeviazione);
        const modificatoreScudo = prendiMassimo(modificatoriScudo);
        const modificatoreMagici = prendiMassimo(modificatoriMagici);
        const modificatoreBase = statisticaBase?.modificatore ?? 0;

        const CA = bonusVALORE + modificatoreSchivare + modificatoreArmatura + modificatoreNaturale + modificatoreDeviazione + modificatoreScudo + modificatoreMagici + modificatoreBase;
        const CA_CONTATTO = modificatoreSchivare + modificatoreDeviazione + modificatoreMagici + modificatoreBase;
        const CA_SORPRESO = modificatoreArmatura + modificatoreNaturale + modificatoreDeviazione + modificatoreScudo + modificatoreMagici;

        if (stat.stat.id === 'CA') {
            return {
                modificatore: CA,
                modificatori: {
                    schivare: modificatoriSchivare,
                    armatura: modificatoriArmatura,
                    naturale: modificatoriNaturale,
                    deviazione: modificatoriDeviazione,
                    scudo: modificatoriScudo,
                    magici: modificatoriMagici,
                    valore: modificatoriValore,
                },
                valori: {
                    schivare: modificatoreSchivare,
                    armatura: modificatoreArmatura,
                    naturale: modificatoreNaturale,
                    deviazione: modificatoreDeviazione,
                    scudo: modificatoreScudo,
                    magici: modificatoreMagici,
                    base: modificatoreBase
                }
            }
        }
        if (stat.stat.id === 'CAC') {
            return {
                modificatore: CA_CONTATTO,
                modificatori: {
                    schivare: modificatoriSchivare,
                    deviazione: modificatoriDeviazione,
                    magici: modificatoriMagici,
                    valore: modificatoriValore,
                },
                valori: {
                    schivare: modificatoreSchivare,
                    deviazione: modificatoreDeviazione,
                    magici: modificatoreMagici,
                    base: modificatoreBase
                }
            }
        }
        if (stat.stat.id === 'CAS') {
            return {
                modificatore: CA_SORPRESO,
                modificatori: {
                    armatura: modificatoriArmatura,
                    naturale: modificatoriNaturale,
                    deviazione: modificatoriDeviazione,
                    scudo: modificatoriScudo,
                    magici: modificatoriMagici,
                },
                valori: {
                    armatura: modificatoreArmatura,
                    naturale: modificatoreNaturale,
                    deviazione: modificatoreDeviazione,
                    scudo: modificatoreScudo,
                    magici: modificatoreMagici,
                }
            }
        }

    }

}

export function testoModificatore(mod: number) {
    if (mod >= 0) return "+" + mod;
    return mod;
}

export function getAllItems(personaggio) {
    let result = [];
    personaggio.items.forEach(item => {
        result.push(...getAllItemsFromItem(item))
    });

    const classiConConteggio = Object.values(
        result
            .filter(it => it.tipo === TIPO_ITEM.CLASSE)
            .reduce((acc, it) => {
                if (!acc[it.id]) {
                    acc[it.id] = {
                        ...it,
                        count: 1,
                        child: [...(it.child ?? [])], // copia child
                    };
                } else {
                    acc[it.id].count += 1;
                }
                return acc;
            }, {} as Record<number, any>)
    );

    // Per ogni classe raggruppata, analizza gli avanzamenti
    for (const cls of classiConConteggio) {
        const {count, avanzamento} = cls;

        if (!Array.isArray(avanzamento)) continue;

        for (const avz of avanzamento) {
            if (avz.livello <= count && avz.itemTarget) {
                // evita duplicati
                const presente = cls.child?.some(link => link.itemTarget?.id === avz.itemTarget.id);
                if (!presente) {
                    cls.child.push({itemTarget: avz.itemTarget});
                }
            }
        }
    }

    result = result.filter(itm => itm.tipo !== TIPO_ITEM.CLASSE);
    classiConConteggio.forEach(classe => result.push(...getAllItemsFromItem(classe)));


    console.log('OGGETTI RILEVATI DA GETALLITEMS', result);
    return result;
}

export function getAllItemsFromItem(item, visited = new Set()): any[] {
    if (!item || visited.has(item.id)) return [];

    visited.add(item.id);

    const result = [item]; // includi l'item corrente

    if (Array.isArray(item.child)) {
        for (const link of item.child) {
            const childItem = link.itemTarget;
            result.push(...getAllItemsFromItem(childItem, visited));
        }
    }

    return result;
}



