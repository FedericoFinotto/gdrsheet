// tipo generico per voce con id, nome, tipo e flag enabled
export interface Item {
    id: number;
    nome: string;
    tipo: string;
    enabled: boolean | null;
}

// estende Voce per gli incantesimi (hanno cd e livello)
export interface IncantesimoItem extends Item {
    cd: number | null;
    livello: number;
}

// container principale
export interface ItemsPersonaggio {
    abilita: Item[];
    talenti: Item[];
    oggetti: Item[];
    consumabili: Item[];
    armi: Item[];
    munizioni: Item[];
    equipaggiamento: Item[];
    classi: Item[];
    razze: Item[];
    attacchi: Item[];
    livelli: Item[];
    maledizioni: Item[];
    incantesimi: IncantesimoItem[];
    trasformazioni: Item[];
}
