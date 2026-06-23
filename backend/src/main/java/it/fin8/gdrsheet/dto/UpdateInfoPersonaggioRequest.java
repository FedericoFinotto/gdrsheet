package it.fin8.gdrsheet.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class UpdateInfoPersonaggioRequest {
    private String nome;
    private Map<String, String> info;
}
