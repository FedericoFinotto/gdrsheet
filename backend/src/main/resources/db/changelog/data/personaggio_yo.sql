-- Inserimento personaggio iniziale
INSERT INTO utente (id, username, password, name, ruolo)
VALUES (3, 'finotto', 'finotto', 'finotto', 'GIOCATORE'::tipo_ruolo);

INSERT INTO personaggio (id, nome, mondo_id)
VALUES (1, 'Yo', 1);

INSERT INTO items (id, nome, tipo, descrizione, personaggio_id, id_sistema, id_mondo)
VALUES (1, 'Changeling', 'RAZZA'::tipo_item, 'Changeling', 1, 1, NULL),
       (2, 'Druido', 'CLASSE'::tipo_item, 'Druido', 1, 1, NULL),
       (3, 'Balbuzie', 'ALTRO'::tipo_item, 'Balbuzie', 1, NULL, NULL);

INSERT INTO permessi_personaggi (id_utente, id_personaggio, tipo)
VALUES (1, 1, 'W'::tipo_permesso);

INSERT INTO stat_value (personaggio_id, stat_id, valore, mod)
VALUES (1, 'FOR', 19, null),
       (1, 'DES', 19, null),
       (1, 'COS', 20, null),
       (1, 'INT', 20, null),
       (1, 'SAG', 22, null),
       (1, 'CAR', 18, null),
       (1, 'AB1', 0, 'DES'),
       (1, 'AB2', 0, 'CAR'),
       (1, 'AB3', 0, 'INT'),
       (1, 'AB4', 0, 'DES'),
       (1, 'AB5', 0, 'SAG'),
       (1, 'AB6', 0, 'CAR'),
       (1, 'AB7', 0, 'DES'),
       (1, 'AB8', 0, 'INT'),
       (1, 'AB9', 0, 'COS'),
       (1, 'AB10', 0, 'INT'),
       (1, 'AB11', 0, 'INT'),
       (1, 'AB12', 0, 'CAR'),
       (1, 'AB13', 0, 'INT'),
       (1, 'AB14', 0, 'DES'),
       (1, 'AB15', 0, 'INT'),
       (1, 'AB16', 0, 'SAG'),
       (1, 'AB17', 0, 'CAR'),
       (1, 'AB18', 0, 'CAR'),
       (1, 'AB19', 0, 'DES'),
       (1, 'AB20', 0, 'DES'),
       (1, 'AB21', 0, 'FOR'),
       (1, 'AB22', 0, 'SAG'),
       (1, 'AB23', 0, 'SAG'),
       (1, 'AB24', 0, 'SAG'),
       (1, 'AB25', 0, 'CAR'),
       (1, 'AB26', 0, 'CAR'),
       (1, 'AB27', 0, 'DES'),
       (1, 'AB28', 0, 'FOR'),
       (1, 'AB29', 0, 'INT'),
       (1, 'AB30', 0, 'FOR'),
       (1, 'AB31', 0, 'DES'),
       (1, 'AB32', 0, 'SAG'),
       (1, 'AB33', 0, 'DES'),
       (1, 'AB34', 0, 'CAR'),
       (1, 'AB35', 0, 'INT'),
       (1, 'AB36', 0, NULL),
       (1, 'PF', 0, NULL),
       (1, 'PFMAX', 0, NULL),
       (1, 'PFTEMP', 0, NULL),
       (1, 'CA', 0, NULL),
       (1, 'CAC', 0, NULL),
       (1, 'CAS', 0, NULL),
       (1, 'TMP', 0, 'COS'),
       (1, 'RFL', 0, 'DES'),
       (1, 'VLT', 0, 'SAG'),
       (1, 'BAB', 0, NULL),
       (1, 'INIT', 0, 'DES'),
       (1, 'LTT', 0, NULL),
       (1, 'MSC', 0, NULL),
       (1, 'GTT', 0, NULL);

INSERT INTO modificatori (id_item, id_stat, valore, nota)
VALUES (3, 'AB12', '-4', ''),
       (3, 'AB17', '-4', ''),
       (3, 'AB26', '-4', ''),
       (3, 'AB32', '+4', ''),
--       CONOSCENZE NAT (3, 'AB26', '+4', ''),
       (3, 'AB30', '+4', ''),
       (3, 'AB21', '+4', '');
