INSERT INTO items (nome, tipo, descrizione, personaggio_id, id_sistema, id_mondo)
VALUES ('Cambiare forma minore', 'ABILITA',
        'i cangianti hanno l''abilità soprannaurale di modificare il loro aspetto come se stessero usando un incantesimo di camuffare se stessi che ha effetto sugli abiti ma solo sul corpo. Questa abilità non è un effetto illusorio, ma un''alterazione fisica minore dei tratti facciali, colore della pelle e taglia del cangiante nei limiti di quello che viene descritto nell''incantesimo. Un cangiante può utilizzare questa abilità a volontà, e dura fino a quando il cangiante non cambia di nuovo. Un cangiante torna alla sua forma naturale una volta morto. Un incantesimo visione del vero rivela la sua forma naturale. Quando usa questa abilità per camuffarsi riceve un bonus di circostanza di +10 alle prove di camuffare. Cambiare forma è un''azione di round completo.',
        null, 1, null),
       ('Changeling', 'RAZZA', 'Changeling', null, 1, NULL);

WITH changeling AS (SELECT id
                    FROM items
                    WHERE nome = 'Changeling')
INSERT
INTO modificatori (id_item,
                   id_stat,
                   valore,
                   always,
                   nota,
                   tipo)
SELECT c.id, 'AB26', '+2', true, NULL, 'VALORE'
FROM changeling c -- RAGGIRARE
UNION ALL
SELECT c.id, 'AB17', '+2', true, NULL, 'VALORE'
FROM changeling c -- INTIMIDIRE
UNION ALL
SELECT c.id, 'AB23', '+2', true, NULL, 'VALORE'
FROM changeling c -- PERCEPIRE INTENZIONI
UNION ALL
SELECT c.id, 'VLT', '+2', false, 'Contro ammaliamento e sonno', 'VALORE'
FROM changeling c -- VOLONTÀ
;


WITH changeling AS (SELECT id AS source_id
                    FROM items
                    WHERE nome = 'Changeling')
INSERT
INTO collegamento (id_item_source, id_item_target)
SELECT c.source_id,
       t.id
FROM changeling c
         CROSS JOIN LATERAL (
    SELECT id
    FROM items
    WHERE nome = 'Cambiare forma minore'
    ) AS t;

INSERT INTO item_label (id_item, label, valore)
VALUES ((select id from items where nome = 'Changeling'), 'ABCLASSE', 'AB36'),
       ((select id from items where nome = 'Changeling'), 'TAGLIA', '0');
