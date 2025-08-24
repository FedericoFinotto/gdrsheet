export type UpdatePreparedRequest = {
    idPersonaggio: number;
    idClasse: number;
    spellList: string;
    livello: number;
    prepared: Record<number, number>;
};