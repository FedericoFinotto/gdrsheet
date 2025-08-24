import {ClassLevel} from "./ClassLevel";

export interface UpdateSpellUsageRequest {
    idPersonaggio?: number;
    newUsage?: number;
    spellId?: number;
}