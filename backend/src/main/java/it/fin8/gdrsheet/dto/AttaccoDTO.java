package it.fin8.gdrsheet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AttaccoDTO extends ItemDTO {
    /** "TPC" o "TS": quale modalità di risoluzione usa l'attacco. */
    private String tipoRisoluzione;
    private String attacco;      // formula TPC, solo se tipoRisoluzione = TPC
    private String nomeItem;
    private String tiroSalvezza;   // tipo di TS, solo se tipoRisoluzione = TS
    private String tiroSalvezzaCd; // formula CD, solo se tipoRisoluzione = TS
    private String range;
    private List<DannoDTO> danni;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DannoDTO {
        private String formula;
        private String tipo;
    }
}
