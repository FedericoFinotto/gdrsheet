INSERT INTO items (nome, tipo, descrizione, personaggio_id, id_sistema, id_mondo)
VALUES ('Cambiare forma minore', 'ABILITA',
        'i cangianti hanno l''abilità soprannaurale di modificare il loro aspetto come se stessero usando un incantesimo di camuffare se stessi che ha effetto sugli abiti ma solo sul corpo. Questa abilità non è un effetto illusorio, ma un''alterazione fisica minore dei tratti facciali, colore della pelle e taglia del cangiante nei limiti di quello che viene descritto nell''incantesimo. Un cangiante può utilizzare questa abilità a volontà, e dura fino a quando il cangiante non cambia di nuovo. Un cangiante torna alla sua forma naturale una volta morto. Un incantesimo visione del vero rivela la sua forma naturale. Quando usa questa abilità per camuffarsi riceve un bonus di circostanza di +10 alle prove di camuffare. Cambiare forma è un''azione di round completo.',
        null, 1, null)
;


INSERT INTO item_label (id_item, label, valore)
VALUES ((select id from items where nome = 'Cangiante'), 'ABCLASSE', 'AB36'),
       ((select id from items where nome = 'Cangiante'), 'TAGLIA', '0');

INSERT INTO items (nome, tipo, descrizione, personaggio_id, id_sistema, id_mondo)
VALUES ('Cangiante', 'CLASSE', null, null, 1, 1);

INSERT INTO items (nome, tipo, descrizione, personaggio_id, id_sistema, id_mondo)
VALUES ('LIVELLO 1 CANGIANTE', 'AVANZAMENTO', null, null, 1, 1),
       ('LIVELLO 2 CANGIANTE', 'AVANZAMENTO', null, null, 1, 1),
       ('LIVELLO 3 CANGIANTE', 'AVANZAMENTO', null, null, 1, 1),
       ('LIVELLO 4 CANGIANTE', 'AVANZAMENTO', null, null, 1, 1),
       ('LIVELLO 5 CANGIANTE', 'AVANZAMENTO', null, null, 1, 1),
       ('LIVELLO 6 CANGIANTE', 'AVANZAMENTO', null, null, 1, 1),
       ('LIVELLO 7 CANGIANTE', 'AVANZAMENTO', null, null, 1, 1),
       ('LIVELLO 8 CANGIANTE', 'AVANZAMENTO', null, null, 1, 1)
;

INSERT into avanzamento (id_item_source, id_item_target, livello)
VALUES ((SELECT id from items where nome = 'Cangiante'), (Select id from items where nome = 'LIVELLO 1 CANGIANTE'),
        '1'),
       ((SELECT id from items where nome = 'Cangiante'), (Select id from items where nome = 'Cambiare forma minore'),
        '1'),
       ((SELECT id from items where nome = 'Cangiante'), (Select id from items where nome = 'LIVELLO 2 CANGIANTE'),
        '2'),
       ((SELECT id from items where nome = 'Cangiante'), (Select id from items where nome = 'LIVELLO 3 CANGIANTE'),
        '3'),
       ((SELECT id from items where nome = 'Cangiante'), (Select id from items where nome = 'LIVELLO 4 CANGIANTE'),
        '4'),
       ((SELECT id from items where nome = 'Cangiante'), (Select id from items where nome = 'LIVELLO 5 CANGIANTE'),
        '5'),
       ((SELECT id from items where nome = 'Cangiante'), (Select id from items where nome = 'LIVELLO 6 CANGIANTE'),
        '6'),
       ((SELECT id from items where nome = 'Cangiante'), (Select id from items where nome = 'LIVELLO 7 CANGIANTE'),
        '7'),
       ((SELECT id from items where nome = 'Cangiante'), (Select id from items where nome = 'LIVELLO 8 CANGIANTE'),
        '8');


INSERT
INTO modificatori (id_item,
                   id_stat,
                   valore,
                   always,
                   nota,
                   tipo)
VALUES ((SELECT id from items where nome = 'LIVELLO 1 CANGIANTE'), (SELECT id from stats where label = 'Raggirare'),
        '+2', true,
        NULL, 'VALORE'),
       ((SELECT id from items where nome = 'LIVELLO 1 CANGIANTE'), (SELECT id from stats where label = 'Intimidire'),
        '+2', true,
        NULL, 'VALORE'),
       ((SELECT id from items where nome = 'LIVELLO 1 CANGIANTE'),
        (SELECT id from stats where label = 'Percepire Intenzioni'),
        '+2', true, NULL, 'VALORE'),
       ((SELECT id from items where nome = 'LIVELLO 1 CANGIANTE'), (SELECT id from stats where label = 'Volonta'), '+2',
        true,
        'Contro ammaliamento e sonno', 'VALORE')
;

