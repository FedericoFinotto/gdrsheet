-- Inserimento personaggio iniziale
INSERT INTO utente (id, username, password, name, ruolo)
VALUES (3, 'finotto', 'finotto', 'finotto', 'GIOCATORE'::tipo_ruolo);

INSERT INTO personaggio (id, nome, mondo_id)
VALUES (1, 'Yo', 1);

INSERT INTO items (id, nome, tipo, descrizione, personaggio_id, id_sistema, id_mondo)
VALUES (1, 'Changeling', 'RAZZA'::tipo_item, 'Changeling', 1, 1, NULL),
       (2, 'Druido', 'CLASSE'::tipo_item, 'Druido', 1, 1, NULL);

INSERT INTO permessi_personaggi (id_utente, id_personaggio, tipo)
VALUES (1, 1, 'W'::tipo_permesso);

INSERT INTO stat_value (personaggio_id, stat_id, valore)
VALUES (1, 'FOR', 40);

INSERT INTO modificatori (id_item, id_stat, valore, nota)
VALUES (1, 'FOR', '2', 'TEST');
