package it.fin8.gdrsheet.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Coppie label/valore legate a un party, stessa logica di item_label/personaggio_label/
 * collegamento_label/utente_label. Primo utilizzo: GIOCATORI (0/1) — distingue i party di
 * personaggi giocanti (usati per liste di visibilità N-party) dagli altri party (es. NPC/staging).
 */
@Getter
@Setter
@Entity
@Table(name = "party_label")
public class PartyLabel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_party", nullable = false)
    private Party party;

    @Column(name = "label", length = Integer.MAX_VALUE)
    private String label;

    @Column(name = "valore", length = Integer.MAX_VALUE)
    private String valore;
}
