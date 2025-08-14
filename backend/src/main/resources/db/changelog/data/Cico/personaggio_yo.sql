INSERT INTO personaggio (nome, party_id)
VALUES ('Qui', 1);

INSERT INTO items (nome, tipo, descrizione, personaggio_id, id_sistema, id_mondo)
VALUES ('Livello 0', 'LIVELLO', 'LIVELLO 0 QUI', (select id from personaggio where nome = 'Qui'), 1, 1),
       ('Livello 1', 'LIVELLO', 'LIVELLO 1 QUI', (select id from personaggio where nome = 'Qui'), 1, 1),
       ('Livello 2', 'LIVELLO', 'LIVELLO 2 QUI', (select id from personaggio where nome = 'Qui'), 1, 1),
       ('Livello 3', 'LIVELLO', 'LIVELLO 3 QUI', (select id from personaggio where nome = 'Qui'), 1, 1),
       ('Livello 4', 'LIVELLO', 'LIVELLO 4 QUI', (select id from personaggio where nome = 'Qui'), 1, 1),
       ('Livello 5', 'LIVELLO', 'LIVELLO 5 QUI', (select id from personaggio where nome = 'Qui'), 1, 1),
       ('Balbuzie', 'MALEDIZIONE', 'Balbuzie', (select id from personaggio where nome = 'Qui'), NULL, NULL),
       ('FromCompendio', 'ALTRO', 'FromCompendio', (select id from personaggio where nome = 'Qui'), 1, 1),
       ('PreparedSpell', 'ALTRO', 'PreparedSpell Qui', (select id from personaggio where nome = 'Qui'), 1, 1),
       ('Papera mostruosa', 'TRASFORMAZIONE', NULL, (select id from personaggio where nome = 'Qui'), 1, 1),
       ('Pugno', 'ATTACCO', NULL, NULL, 1, 1),
       ('Beccata', 'ATTACCO', NULL, NULL, 1, 1),
       ('Soffio (Suono)', 'ATTACCO', NULL, NULL, 1, 1)
;

INSERT INTO item_label (id_item, label, valore)
VALUES ((SELECT id FROM items WHERE nome = 'Pugno'), 'TPC', '@BAB+@FOR'),
       ((SELECT id FROM items WHERE nome = 'Pugno'), 'TPD', '1d6+@FOR'),
       ((SELECT id FROM items WHERE nome = 'Beccata'), 'TPC', '@BAB+@FOR'),
       ((SELECT id FROM items WHERE nome = 'Beccata'), 'TPD', '1d8+@FOR/2'),
       ((SELECT id FROM items WHERE nome = 'Soffio (Suono)'), 'TPD', '3d6'),
       ((select id from items where nome = 'Papera mostruosa'), 'TAGLIA', '1'),
       ((SELECT id from items where descrizione = 'LIVELLO 0 QUI'), 'LINGUE', '1894,1909,1895,1896,1897,1905');

INSERT INTO collegamento (id_item_source, id_item_target)
VALUES ((SELECT id from items where descrizione = 'LIVELLO 0 QUI'), (SELECT id from items where nome = 'Changeling')),
       ((SELECT id FROM items where descrizione = 'LIVELLO 1 QUI'), (SELECT id FROM items where nome = 'Druido')),
       ((SELECT id FROM items where descrizione = 'LIVELLO 2 QUI'), (SELECT id FROM items where nome = 'Druido')),
       ((SELECT id FROM items where descrizione = 'LIVELLO 3 QUI'), (SELECT id FROM items where nome = 'Druido')),
       ((SELECT id FROM items where descrizione = 'LIVELLO 4 QUI'), (SELECT id FROM items where nome = 'Druido')),
       ((SELECT id FROM items where descrizione = 'LIVELLO 5 QUI'), (SELECT id FROM items where nome = 'Druido')),
       ((SELECT id FROM items where nome = 'FromCompendio'),
        (SELECT id FROM items where nome = 'Fucile Ammazzadivinita')),
       ((SELECT id FROM items where nome = 'FromCompendio'),
        (SELECT id FROM items where nome = 'Armatura in Pelle di Xenomorfo')),
       ((SELECT id FROM items where nome = 'FromCompendio'), (SELECT id FROM items where nome = 'Scudo in legno')),
       ((SELECT id FROM items where nome = 'FromCompendio'), (SELECT id FROM items where nome = 'Tantoo')),
       ((SELECT id FROM items where nome = 'FromCompendio'), (SELECT id FROM items where nome = 'Veste del druido')),
       ((SELECT id FROM items where nome = 'FromCompendio'), (SELECT id FROM items where nome = 'Anello Camaleonte')),
       ((SELECT id FROM items where nome = 'FromCompendio'),
        (SELECT id FROM items where nome = 'Anello Camminare sull''acqua')),
       ((SELECT id FROM items where nome = 'FromCompendio'),
        (SELECT id FROM items where nome = 'Sassolino ritornante')),
       ((SELECT id FROM items where nome = 'FromCompendio'), (SELECT id FROM items where nome = 'Extra Wild Shape')),
       ((SELECT id FROM items where nome = 'Papera mostruosa'), (SELECT id FROM items where nome = 'Pugno')),
       ((SELECT id FROM items where nome = 'Papera mostruosa'), (SELECT id FROM items where nome = 'Beccata')),
       ((SELECT id FROM items where nome = 'Papera mostruosa'), (SELECT id FROM items where nome = 'Soffio (Suono)')),
       ((SELECT id FROM items where nome = 'FromCompendio'), (SELECT id FROM items where nome = 'Resistenza Fisica')),
       ((SELECT id FROM items where descrizione = 'LIVELLO 0 QUI'), (SELECT id FROM items where nome = 'Changeling'))
;

INSERT INTO stat_value (personaggio_id, stat_id, valore, mod, classe, addestramento)
VALUES ((select id from personaggio where nome = 'Qui'), 'FOR', '0', NULL, false, false),
       ((select id from personaggio where nome = 'Qui'), 'DES', '0', NULL, false, false),
       ((select id from personaggio where nome = 'Qui'), 'COS', '0', NULL, false, false),
       ((select id from personaggio where nome = 'Qui'), 'INT', '0', NULL, false, false),
       ((select id from personaggio where nome = 'Qui'), 'SAG', '0', NULL, false, false),
       ((select id from personaggio where nome = 'Qui'), 'CAR', '0', NULL, false, false),
       ((select id from personaggio where nome = 'Qui'), 'AB1', '0', 'DES', false, true),
       ((select id from personaggio where nome = 'Qui'), 'AB2', '0', 'CAR', false, true),
       ((select id from personaggio where nome = 'Qui'), 'AB3', '0', 'INT', false, false),
       ((select id from personaggio where nome = 'Qui'), 'AB4', '0', 'DES', false, false),
       ((select id from personaggio where nome = 'Qui'), 'AB5', '0', 'SAG', false, false),
       ((select id from personaggio where nome = 'Qui'), 'AB6', '0', 'CAR', false, false),
       ((select id from personaggio where nome = 'Qui'), 'AB7', '0', 'DES', false, false),
       ((select id from personaggio where nome = 'Qui'), 'AB8', '0', 'INT', false, false),
       ((select id from personaggio where nome = 'Qui'), 'AB9', '0', 'COS', false, false),
       ((select id from personaggio where nome = 'Qui'), 'CO00', '0', 'INT', false, true),
       ((select id from personaggio where nome = 'Qui'), 'AB11', '0', 'INT', false, true),
       ((select id from personaggio where nome = 'Qui'), 'AB12', '0', 'CAR', false, false),
       ((select id from personaggio where nome = 'Qui'), 'AB13', '0', 'INT', false, true),
       ((select id from personaggio where nome = 'Qui'), 'AB14', '0', 'DES', false, false),
       ((select id from personaggio where nome = 'Qui'), 'AB15', '0', 'INT', false, false),
       ((select id from personaggio where nome = 'Qui'), 'AB16', '0', 'SAG', false, false),
       ((select id from personaggio where nome = 'Qui'), 'AB17', '0', 'CAR', false, false),
       ((select id from personaggio where nome = 'Qui'), 'AB18', '0', 'CAR', false, false),
       ((select id from personaggio where nome = 'Qui'), 'AB19', '0', 'DES', false, false),
       ((select id from personaggio where nome = 'Qui'), 'AB20', '0', 'DES', false, false),
       ((select id from personaggio where nome = 'Qui'), 'AB21', '0', 'FOR', false, false),
       ((select id from personaggio where nome = 'Qui'), 'AB22', '0', 'SAG', false, false),
       ((select id from personaggio where nome = 'Qui'), 'AB23', '0', 'SAG', false, false),
       ((select id from personaggio where nome = 'Qui'), 'PR00', '0', 'SAG', false, true),
       ((select id from personaggio where nome = 'Qui'), 'AB25', '0', 'CAR', false, false),
       ((select id from personaggio where nome = 'Qui'), 'AB26', '0', 'CAR', false, false),
       ((select id from personaggio where nome = 'Qui'), 'AB27', '0', 'DES', false, true),
       ((select id from personaggio where nome = 'Qui'), 'AB28', '0', 'FOR', false, false),
       ((select id from personaggio where nome = 'Qui'), 'AB29', '0', 'INT', false, true),
       ((select id from personaggio where nome = 'Qui'), 'AB30', '0', 'FOR', false, false),
       ((select id from personaggio where nome = 'Qui'), 'AB31', '0', 'DES', false, true),
       ((select id from personaggio where nome = 'Qui'), 'AB32', '0', 'SAG', false, false),
       ((select id from personaggio where nome = 'Qui'), 'AB33', '0', 'DES', false, false),
       ((select id from personaggio where nome = 'Qui'), 'AB34', '0', 'CAR', false, true),
       ((select id from personaggio where nome = 'Qui'), 'AB35', '0', 'INT', false, false),
       ((select id from personaggio where nome = 'Qui'), 'AB36', '0', NULL, false, true),
       ((select id from personaggio where nome = 'Qui'), 'PF', '0', NULL, false, false),
       ((select id from personaggio where nome = 'Qui'), 'PFMAX', '0', NULL, false, false),
       ((select id from personaggio where nome = 'Qui'), 'PFTEMP', '0', NULL, false, false),
       ((select id from personaggio where nome = 'Qui'), 'CA', '0', 'DES', false, false),
       ((select id from personaggio where nome = 'Qui'), 'CAC', '0', 'DES', false, false),
       ((select id from personaggio where nome = 'Qui'), 'CAS', '0', 'DES', false, false),
       ((select id from personaggio where nome = 'Qui'), 'TMP', '0', 'COS', false, false),
       ((select id from personaggio where nome = 'Qui'), 'RFL', '0', 'DES', false, false),
       ((select id from personaggio where nome = 'Qui'), 'VLT', '0', 'SAG', false, false),
       ((select id from personaggio where nome = 'Qui'), 'BAB', '0', NULL, false, false),
       ((select id from personaggio where nome = 'Qui'), 'INIT', '0', 'DES', false, false),
       ((select id from personaggio where nome = 'Qui'), 'LTT', '0', 'FOR', false, false),
       ((select id from personaggio where nome = 'Qui'), 'MSC', '0', 'FOR', false, false),
       ((select id from personaggio where nome = 'Qui'), 'GTT', '0', 'DES', false, false),
       ((select id from personaggio where nome = 'Qui'), 'CO01', '0', 'INT', false, true),
       ((select id from personaggio where nome = 'Qui'), 'CO02', '0', 'INT', false, true),
       ((select id from personaggio where nome = 'Qui'), 'CO03', '0', 'INT', false, true),
       ((select id from personaggio where nome = 'Qui'), 'CO04', '0', 'INT', false, true),
       ((select id from personaggio where nome = 'Qui'), 'CO05', '0', 'INT', false, true),
       ((select id from personaggio where nome = 'Qui'), 'CO06', '0', 'INT', false, true),
       ((select id from personaggio where nome = 'Qui'), 'CO07', '0', 'INT', false, true),
       ((select id from personaggio where nome = 'Qui'), 'CO08', '0', 'INT', false, true),
       ((select id from personaggio where nome = 'Qui'), 'CO09', '0', 'INT', false, true),
       ((select id from personaggio where nome = 'Qui'), 'CO10', '0', 'INT', false, true),
       ((select id from personaggio where nome = 'Qui'), 'CO11', '0', 'INT', false, true),
       ((select id from personaggio where nome = 'Qui'), 'PR01', '0', NULL, false, true),
       ((select id from personaggio where nome = 'Qui'), 'PR02', '0', NULL, false, true),
       ((select id from personaggio where nome = 'Qui'), 'PR03', '0', NULL, false, true),
       ((select id from personaggio where nome = 'Qui'), 'PR04', '0', NULL, false, true),
       ((select id from personaggio where nome = 'Qui'), 'PR05', '0', NULL, false, true),
       ((select id from personaggio where nome = 'Qui'), 'PR06', '0', NULL, false, true),
       ((select id from personaggio where nome = 'Qui'), 'RINC', '0', NULL, false, false),
       ((select id from personaggio where nome = 'Qui'), 'RFIRE', '0', NULL, false, false),
       ((select id from personaggio where nome = 'Qui'), 'RCOLD', '0', NULL, false, false),
       ((select id from personaggio where nome = 'Qui'), 'RTHUN', '0', NULL, false, false),
       ((select id from personaggio where nome = 'Qui'), 'RACID', '0', NULL, false, false),
       ((select id from personaggio where nome = 'Qui'), 'RSOUND', '0', NULL, false, false),
       ((select id from personaggio where nome = 'Qui'), 'RFORCE', '0', NULL, false, false),
       ((select id from personaggio where nome = 'Qui'), 'RPOS', '0', NULL, false, false),
       ((select id from personaggio where nome = 'Qui'), 'RIDDAN', '0', NULL, false, false),
       ((select id from personaggio where nome = 'Qui'), 'RNEG', '0', NULL, false, false),
       ((select id from personaggio where nome = 'Qui'), 'CD', '10', NULL, false, false),
       ((select id from personaggio where nome = 'Qui'), 'REGEN', '0', NULL, false, false),
       ((select id from personaggio where nome = 'Qui'), 'NWS', '0', NULL, true, false)
;

INSERT INTO modificatori (id_item, id_stat, valore, always, tipo)
VALUES ((select id from items where nome = 'Balbuzie'), 'AB12', '-4', true, 'VALORE'),           --DIPLOMAZIA
       ((select id from items where nome = 'Balbuzie'), 'AB17', '-4', true, 'VALORE'),           --INTIMIDIRE
       ((select id from items where nome = 'Balbuzie'), 'AB26', '-4', true, 'VALORE'),           --RAGGIRARE
       ((select id from items where nome = 'Balbuzie'), 'AB32', '+4', true, 'VALORE'),           --SOPRAVVIVENZA
       ((select id from items where nome = 'Balbuzie'), 'CO10', '+4', true, 'VALORE'),           --CONOSCENZE NATURA
       ((select id from items where nome = 'Balbuzie'), 'AB30', '+4', true, 'VALORE'),           --SCALARE
       ((select id from items where nome = 'Balbuzie'), 'AB21', '+4', true, 'VALORE'),           -- NUOTARE
       ((select id from items where descrizione = 'LIVELLO 1 QUI'), 'CO10', '+4', true, 'RANK'), -- CONOSCENZE NATURA
       ((select id from items where descrizione = 'LIVELLO 1 QUI'), 'AB9', '+4', true, 'RANK'),  -- CONCENTRAZIONE
       ((select id from items where descrizione = 'LIVELLO 1 QUI'), 'AB32', '+4', true, 'RANK'), -- SOPRAVVIVENZA
       ((select id from items where descrizione = 'LIVELLO 1 QUI'), 'AB2', '+1', true, 'RANK'),  -- ADDRESTRARE ANIMALI
       ((select id from items where descrizione = 'LIVELLO 1 QUI'), 'AB29', '+4', true, 'RANK'), -- SAPIENZA MAGICA
       ((select id from items where descrizione = 'LIVELLO 1 QUI'), 'AB36', '+1', true, 'RANK'), -- PARLARE LINGUAGGI
       ((select id from items where descrizione = 'LIVELLO 1 QUI'), 'AB16', '+4', true, 'RANK'), -- GUARIRE
       ((select id from items where descrizione = 'LIVELLO 1 QUI'), 'AB21', '+4', true, 'RANK'), -- NUOTARE
       ((select id from items where descrizione = 'LIVELLO 1 QUI'), 'AB5', '+4', true, 'RANK'),  -- ASCOLTARE
       ((select id from items where descrizione = 'LIVELLO 1 QUI'), 'CO09', '+2', true, 'RANK'), -- CONOSCENZA ARCH
       ((select id from items where descrizione = 'LIVELLO 1 QUI'), 'AB22', '+4', true, 'RANK'), -- OSSERVARE
       ((select id from items where descrizione = 'LIVELLO 2 QUI'), 'AB16', '+4', true, 'RANK'), -- GUARIRE
       ((select id from items where descrizione = 'LIVELLO 2 QUI'), 'AB21', '+4', true, 'RANK'), -- NUOTARE
       ((select id from items where descrizione = 'LIVELLO 3 QUI'), 'CO09', '+4', true, 'RANK'), -- CONOSCENZA ARCH
       ((select id from items where descrizione = 'LIVELLO 3 QUI'), 'AB9', '+4', true, 'RANK'),  -- CONCENTRAZIONE
       ((select id from items where descrizione = 'LIVELLO 4 QUI'), 'AB29', '+4', true, 'RANK'), -- SAPIENZA MAGICA
       ((select id from items where descrizione = 'LIVELLO 4 QUI'), 'AB22', '+4', true, 'RANK'), -- OSSERVARE
       ((select id from items where descrizione = 'LIVELLO 5 QUI'), 'AB5', '+4', true, 'RANK'),  -- ASCOLTARE
       ((select id from items where descrizione = 'LIVELLO 5 QUI'), 'CO09', '+4', true, 'RANK'), -- CONOSCENZA ARCH;
       ((select id from items where descrizione = 'LIVELLO 0 QUI'), 'FOR', '19', true, 'BASE'),
       ((select id from items where descrizione = 'LIVELLO 0 QUI'), 'DES', '19', true, 'BASE'),
       ((select id from items where descrizione = 'LIVELLO 0 QUI'), 'COS', '20', true, 'BASE'),
       ((select id from items where descrizione = 'LIVELLO 0 QUI'), 'INT', '20', true, 'BASE'),
       ((select id from items where descrizione = 'LIVELLO 0 QUI'), 'SAG', '24', true, 'BASE'),
       ((select id from items where descrizione = 'LIVELLO 0 QUI'), 'CAR', '18', true, 'BASE'),
       ((select id from items where descrizione = 'LIVELLO 1 QUI'), 'PF', '+8', true, 'VALORE'),
       ((select id from items where descrizione = 'LIVELLO 2 QUI'), 'PF', '+7', true, 'VALORE'),
       ((select id from items where descrizione = 'LIVELLO 3 QUI'), 'PF', '+7', true, 'VALORE'),
       ((select id from items where descrizione = 'LIVELLO 4 QUI'), 'PF', '+7', true, 'VALORE'),
       ((select id from items where descrizione = 'LIVELLO 5 QUI'), 'PF', '+7', true, 'VALORE'),
       ((SELECT id FROM items where nome = 'Papera mostruosa'), 'FOR', '30', true, 'BASE'),
       ((SELECT id FROM items where nome = 'Papera mostruosa'), 'COS', '30', true, 'BASE'),
       ((SELECT id FROM items where nome = 'Papera mostruosa'), 'DES', '18', true, 'BASE'),
       ((SELECT id FROM items where nome = 'Papera mostruosa'), 'VLT', '+3', true, 'VALORE'),
       ((SELECT id FROM items where nome = 'Papera mostruosa'), 'TMP', '+3', true, 'VALORE'),
       ((SELECT id FROM items where nome = 'Papera mostruosa'), 'RFL', '+3', true, 'VALORE'),
       ((SELECT id FROM items where nome = 'Papera mostruosa'), 'CA', '+10', true, 'VALORE'),
       ((SELECT id FROM items where nome = 'Papera mostruosa'), 'CA', '+5', true, 'VALORE'),
       ((SELECT id FROM items where nome = 'Papera mostruosa'), 'RIDDAN', '+10', true, 'VALORE'),
       ((SELECT id FROM items where nome = 'Papera mostruosa'), 'REGEN', '5', true, 'VALORE'),
       ((SELECT id FROM items where nome = 'Papera mostruosa'), 'CD', '+4', true, 'VALORE');


INSERT INTO items (nome, tipo, descrizione, personaggio_id, id_sistema, id_mondo)
VALUES ('Livello 6', 'LIVELLO', 'LIVELLO 6 QUI', (select id from personaggio where nome = 'Qui'), 1, 1);

INSERT INTO collegamento (id_item_source, id_item_target)
VALUES ((SELECT id FROM ITEMS where descrizione = 'LIVELLO 6 QUI'),
        (SELECT id FROM ITEMS WHERE nome = 'Maestro dalle molte forme'));

INSERT INTO collegamento (id_item_source, id_item_target)
VALUES ((SELECT id FROM items where nome = 'FromCompendio'), (SELECT id FROM items where nome = 'Allerta'));

INSERT INTO modificatori (id_item, id_stat, valore, always, nota, tipo)
VALUES ((SELECT id from items where descrizione = 'LIVELLO 2 QUI'), 'AB34', '+1', true, null, 'RANK'),
       ((SELECT id from items where descrizione = 'LIVELLO 3 QUI'), 'AB34', '+1', true, null, 'RANK'),
       ((SELECT id from items where descrizione = 'LIVELLO 4 QUI'), 'AB36', '+1', true, null, 'RANK'),
       ((SELECT id from items where descrizione = 'LIVELLO 5 QUI'), 'AB36', '+1', true, null, 'RANK'),
       ((SELECT id from items where descrizione = 'LIVELLO 6 QUI'), 'AB9', '+1', true, null, 'RANK'),  --CONCENTRAZIONE
       ((SELECT id from items where descrizione = 'LIVELLO 6 QUI'), 'AB6', '+4', true, null, 'RANK'),  --CAMUFFARE
       ((SELECT id from items where descrizione = 'LIVELLO 6 QUI'), 'CO10', '+1', true, null, 'RANK'), --NATURA
       ((SELECT id from items where descrizione = 'LIVELLO 6 QUI'), 'AB2', '+3', true, null,
        'RANK'); --ADDESTRARE ANIMALI

INSERT INTO stat_value (personaggio_id, stat_id, valore, mod, classe, addestramento)
VALUES ((select id from personaggio where nome = 'Qui'), 'PSIO', '0', NULL, false, false);

Insert into items (nome, tipo, descrizione, personaggio_id, id_sistema, id_mondo)
VALUES ('Papera Psionica Viola Leggendaria', 'TRASFORMAZIONE', null, 1, null, 1),
       ('Flagello Mentale', 'ABILITA',
        'Costo: 3 Psio, Danno: 1d10, TS: CD 20 Tempa dimezza, +1 PSIO => +1 CD, +1d10 danni', null, null, 1),
       ('Assorbimento danni', 'ABILITA', '1 PSIO => 5PF', null, null, 1),
       ('Repulsione', 'ABILITA', 'Costo: 5 Psio, Distanza: 3m, TS: CD 20 Forza, +1 PSIO => +2 CD, +3m', null, null, 1),
       ('Telecinesi', 'ABILITA',
        'Costo: 3 Psio, +2 PSIO => +4 Lotta, Questa abilita permette di Spingere, Muovere, Bloccare o far andare oggetti contro un bersaglio. Per prima cosa viene fatta una prova di contatto a distanza, poi una prova di lotta a cui puo aggiungere INTELLIGENZA e livello. Telecinesi mirata su oggetto piccolo -8 TPC, oggetto medio -4 TPC.',
        null, null, 1);

insert into collegamento (id_item_source, id_item_target)
VALUES ((SELECT id from items where nome = 'Papera Psionica Viola Leggendaria'),
        (SELECT id from items where nome = 'Flagello Mentale')),
       ((SELECT id from items where nome = 'Papera Psionica Viola Leggendaria'),
        (SELECT id from items where nome = 'Assorbimento danni')),
       ((SELECT id from items where nome = 'Papera Psionica Viola Leggendaria'),
        (SELECT id from items where nome = 'Repulsione')),
       ((SELECT id from items where nome = 'Papera Psionica Viola Leggendaria'),
        (SELECT id from items where nome = 'Telecinesi'));

insert into modificatori (id_item, id_stat, valore, always, nota, tipo)
VALUES ((SELECT id from items where nome = 'Papera Psionica Viola Leggendaria'), 'INT', '40', true, null, 'BASE'),
       ((SELECT id from items where nome = 'Papera Psionica Viola Leggendaria'), 'SAG', '40', true, null, 'BASE'),
       ((SELECT id from items where nome = 'Papera Psionica Viola Leggendaria'), 'CAR', '40', true, null, 'BASE'),
       ((SELECT id from items where nome = 'Papera Psionica Viola Leggendaria'), 'PSIO', '80', true, null, 'VALORE');

insert into modificatori (id_item, id_stat, valore, always, nota, tipo)
VALUES ((SELECT id from items where descrizione = 'LIVELLO 1 QUI'), 'PR02', '+1', true, null, 'VALORE'),
       ((SELECT id from items where descrizione = 'LIVELLO 2 QUI'), 'PR02', '+1', true, null, 'VALORE'),
       ((SELECT id from items where descrizione = 'LIVELLO 3 QUI'), 'PR02', '+1', true, null, 'VALORE'),
       ((SELECT id from items where descrizione = 'LIVELLO 4 QUI'), 'PR02', '+1', true, null, 'VALORE'),
       ((SELECT id from items where descrizione = 'LIVELLO 5 QUI'), 'PR02', '+1', true, null, 'VALORE'),
       ((SELECT id from items where descrizione = 'LIVELLO 6 QUI'), 'PR02', '+1', true, null, 'VALORE');

insert into modificatori (id_item, id_stat, valore, always, nota, tipo)
VALUES ((SELECT id from items where descrizione = 'LIVELLO 6 QUI'), 'PF', '+6', true, null, 'VALORE');

