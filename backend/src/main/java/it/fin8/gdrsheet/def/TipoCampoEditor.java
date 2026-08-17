package it.fin8.gdrsheet.def;

/**
 * Tipo di controllo UI per un "campo libero" ({@link it.fin8.gdrsheet.entity.MondoTipoItemCampo}).
 * Rispecchia le varianti già gestite da BaseItemEditor.vue per i {@code CampoLabel}: {@code null}/
 * assente = input testo semplice.
 */
public enum TipoCampoEditor {
    TESTO,
    TEXTAREA,
    CHECKBOX,
    SELECT,
    DATETIME,
}
