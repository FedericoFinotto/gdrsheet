-- Inserimento personaggio iniziale
INSERT INTO utente (id, username, password, name, ruolo)
VALUES (3, 'finotto', 'finotto', 'finotto', 'GIOCATORE'::tipo_ruolo);

INSERT INTO personaggio (id, nome, mondo_id)
VALUES (1, 'Yo', 1);

INSERT INTO items (nome, tipo, descrizione, personaggio_id, id_sistema, id_mondo)
VALUES ('Changeling', 'RAZZA'::tipo_item, 'Changeling', 1, 1, NULL),
       ('Druido', 'CLASSE'::tipo_item, 'Druido', 1, 1, NULL),
       ('Balbuzie', 'ALTRO'::tipo_item, 'Balbuzie', 1, NULL, NULL),
       ('Livello 1', 'LIVELLO'::tipo_item, 'LIVELLO 1', 1, 1, 1),
       ('Livello 2', 'LIVELLO'::tipo_item, 'LIVELLO 2', 1, 1, 1),
       ('Livello 3', 'LIVELLO'::tipo_item, 'LIVELLO 3', 1, 1, 1),
       ('Livello 4', 'LIVELLO'::tipo_item, 'LIVELLO 4', 1, 1, 1),
       ('Livello 5', 'LIVELLO'::tipo_item, 'LIVELLO 5', 1, 1, 1),
       ('Fucile Ammazzadivinita', 'ARMA'::tipo_item, 'Fucile Ammazzadivinita', 1, 1, 1),
       ('Maledizione del fucile Ammazzadivinita', 'MALEDIZIONE'::tipo_item, 'Maledizione del fucile Ammazzadivinita',
        NULL, 1, 1),
       ('Olfatto Acuto', 'ABILITA'::tipo_item, 'Olfatto Acuto', null, 1, 1);

INSERT INTO permessi_personaggi (id_utente, id_personaggio, tipo)
VALUES (1, 1, 'W'::tipo_permesso);

INSERT INTO stat_value (personaggio_id, stat_id, valore, mod, classe, addestramento)
VALUES (1, 'FOR', '19', null, false, false),
       (1, 'DES', '19', null, false, false),
       (1, 'COS', '20', null, false, false),
       (1, 'INT', '20', null, false, false),
       (1, 'SAG', '22', null, false, false),
       (1, 'CAR', '18', null, false, false),
       (1, 'AB1', '0', 'DES', false, true),
       (1, 'AB2', '0', 'CAR', true, true),
       (1, 'AB3', '0', 'INT', false, false),
       (1, 'AB4', '0', 'DES', false, false),
       (1, 'AB5', '0', 'SAG', true, false),
       (1, 'AB6', '0', 'CAR', false, false),
       (1, 'AB7', '0', 'DES', true, false),
       (1, 'AB8', '0', 'INT', false, false),
       (1, 'AB9', '0', 'COS', true, false),
       (1, 'CO00', '0', 'INT', false, true),
       (1, 'AB11', '0', 'INT', false, true),
       (1, 'AB12', '0', 'CAR', true, false),
       (1, 'AB13', '0', 'INT', false, true),
       (1, 'AB14', '0', 'DES', false, false),
       (1, 'AB15', '0', 'INT', false, false),
       (1, 'AB16', '0', 'SAG', true, false),
       (1, 'AB17', '0', 'CAR', false, false),
       (1, 'AB18', '0', 'CAR', false, false),
       (1, 'AB19', '0', 'DES', false, false),
       (1, 'AB20', '0', 'DES', false, false),
       (1, 'AB21', '0', 'FOR', true, false),
       (1, 'AB22', '0', 'SAG', true, false),
       (1, 'AB23', '0', 'SAG', false, false),
       (1, 'PR00', '0', 'SAG', false, true),
       (1, 'AB25', '0', 'CAR', false, false),
       (1, 'AB26', '0', 'CAR', false, false),
       (1, 'AB27', '0', 'DES', false, true),
       (1, 'AB28', '0', 'FOR', false, false),
       (1, 'AB29', '0', 'INT', true, true),
       (1, 'AB30', '0', 'FOR', false, false),
       (1, 'AB31', '0', 'DES', false, true),
       (1, 'AB32', '0', 'SAG', true, false),
       (1, 'AB33', '0', 'DES', false, false),
       (1, 'AB34', '0', 'CAR', false, true),
       (1, 'AB35', '0', 'INT', false, false),
       (1, 'AB36', '0', NULL, false, true),
       (1, 'PF', '0', NULL, false, false),
       (1, 'PFMAX', '0', NULL, false, false),
       (1, 'PFTEMP', '0', NULL, false, false),
       (1, 'CA', '0', NULL, false, false),
       (1, 'CAC', '0', NULL, false, false),
       (1, 'CAS', '0', NULL, false, false),
       (1, 'TMP', '0', 'COS', false, false),
       (1, 'RFL', '0', 'DES', false, false),
       (1, 'VLT', '0', 'SAG', false, false),
       (1, 'BAB', '0', NULL, false, false),
       (1, 'INIT', '0', 'DES', false, false),
       (1, 'LTT', '0', NULL, false, false),
       (1, 'MSC', '0', NULL, false, false),
       (1, 'GTT', '0', NULL, false, false),
       (1, 'CO01', '0', 'INT', false, true),
       (1, 'CO02', '0', 'INT', false, true),
       (1, 'CO03', '0', 'INT', false, true),
       (1, 'CO04', '0', 'INT', false, true),
       (1, 'CO05', '0', 'INT', false, true),
       (1, 'CO06', '0', 'INT', false, true),
       (1, 'CO07', '0', 'INT', false, true),
       (1, 'CO08', '0', 'INT', false, true),
       (1, 'CO09', '0', 'INT', false, true),
       (1, 'CO10', '0', 'INT', true, true),
       (1, 'CO11', '0', 'INT', false, true),
       (1, 'PR01', '0', NULL, false, true),
       (1, 'PR02', '0', NULL, false, true),
       (1, 'PR03', '0', NULL, false, true),
       (1, 'PR04', '0', NULL, false, true),
       (1, 'PR05', '0', NULL, false, true),
       (1, 'PR06', '0', NULL, false, true);

INSERT INTO modificatori (id_item, id_stat, valore, always, tipo)
VALUES (3, 'AB12', '-4', true, 'VALORE'::tipo_modificatore),
       (3, 'AB17', '-4', true, 'VALORE'::tipo_modificatore),
       (3, 'AB26', '-4', true, 'VALORE'::tipo_modificatore),
       (3, 'AB32', '+4', true, 'VALORE'::tipo_modificatore),
       (3, 'CO10', '+4', true, 'VALORE'::tipo_modificatore),
       (3, 'AB30', '+4', true, 'VALORE'::tipo_modificatore),
       (3, 'AB21', '+4', true, 'VALORE'::tipo_modificatore),
       (1, 'AB26', '+2', true, 'VALORE'::tipo_modificatore),
       (1, 'AB17', '+2', true, 'VALORE'::tipo_modificatore),
       (1, 'AB23', '+2', true, 'VALORE'::tipo_modificatore),
       (6, 'CAR', '-2', true, 'VALORE'::tipo_modificatore), -- CARISMA
       (4, 'AB9', '+4', true, 'RANK'::tipo_modificatore),   -- CONCENTRAZIONE
       (4, 'AB32', '+4', true, 'RANK'::tipo_modificatore),  -- SOPRAVVIVENZA
       (4, 'AB2', '+1', true, 'RANK'::tipo_modificatore),   -- ADDRESTRARE ANIMALI
       (4, 'AB29', '+4', true, 'RANK'::tipo_modificatore),  -- SAPIENZA MAGICA
       (4, 'AB36', '+1', true, 'RANK'::tipo_modificatore),  -- PARLARE LINGUAGGI
       (4, 'AB16', '+4', true, 'RANK'::tipo_modificatore),  -- GUARIRE
       (4, 'AB21', '+4', true, 'RANK'::tipo_modificatore),  -- NUOTARE
       (4, 'AB5', '+4', true, 'RANK'::tipo_modificatore),   -- ASCOLTARE
       (4, 'CO09', '+2', true, 'RANK'::tipo_modificatore),  -- CONOSCENZA ARCH
       (4, 'AB22', '+4', true, 'RANK'::tipo_modificatore),  -- OSSERVARE
       (5, 'AB16', '+4', true, 'RANK'::tipo_modificatore),  -- GUARIRE
       (5, 'AB21', '+4', true, 'RANK'::tipo_modificatore),  -- NUOTARE
       (6, 'CO09', '+4', true, 'RANK'::tipo_modificatore),  -- CONOSCENZA ARCH
       (6, 'AB9', '+4', true, 'RANK'::tipo_modificatore),   -- CONCENTRAZIONE
       (7, 'AB29', '+4', true, 'RANK'::tipo_modificatore),  -- SAPIENZA MAGICA
       (7, 'AB22', '+4', true, 'RANK'::tipo_modificatore),  -- OSSERVARE
       (8, 'AB5', '+4', true, 'RANK'::tipo_modificatore),   -- ASCOLTARE
       (8, 'CO09', '+4', true, 'RANK'::tipo_modificatore); -- CONOSCENZA ARCH;

INSERT INTO modificatori (id_item, id_stat, valore, always, nota, tipo)
VALUES (1, 'VLT', '+2', false, 'Contro addomentamento e ammaliamento', 'VALORE'::tipo_modificatore);

INSERT INTO collegamento (id_item_source, id_item_target)
VALUES (9, 10),
       (10, 11),
       (4, 2),
       (5, 2),
       (6, 2),
       (7, 2),
       (8, 2);
