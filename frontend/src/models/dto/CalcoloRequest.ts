import {DatiPersonaggio} from "./DatiPersonaggio";

export interface CalcoloRequest {
    formula: string;
    datiPersonaggio: DatiPersonaggio;
}