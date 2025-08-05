-- Inserimento personaggio iniziale
INSERT INTO utente (id, username, password, name, ruolo)
VALUES (3, 'finotto', 'finotto', 'finotto', 'GIOCATORE');

INSERT INTO personaggio (id, nome, mondo_id)
VALUES (1, 'Yo', 1);

INSERT INTO permessi_personaggi (id_utente, id_personaggio, tipo)
VALUES (1, 1, 'W');

INSERT INTO items (nome, tipo, descrizione, personaggio_id, id_sistema, id_mondo)
VALUES ('Changeling', 'RAZZA', 'Changeling', 1, 1, NULL),
       ('Druido', 'CLASSE', 'Druido', 1, 1, NULL),
       ('Balbuzie', 'ALTRO', 'Balbuzie', 1, NULL, NULL),
       ('Livello 1', 'LIVELLO', 'LIVELLO 1', 1, 1, 1),
       ('Livello 2', 'LIVELLO', 'LIVELLO 2', 1, 1, 1),
       ('Livello 3', 'LIVELLO', 'LIVELLO 3', 1, 1, 1),
       ('Livello 4', 'LIVELLO', 'LIVELLO 4', 1, 1, 1),
       ('Livello 5', 'LIVELLO', 'LIVELLO 5', 1, 1, 1),
       ('Fucile Ammazzadivinita', 'ARMA', 'Fucile Ammazzadivinita', 1, 1, 1),
       ('Maledizione del fucile Ammazzadivinita', 'MALEDIZIONE', 'Maledizione del fucile Ammazzadivinita',
        NULL, 1, 1),
       ('Olfatto Acuto', 'ABILITA',
        'Olfatto Acuto ti permette di percepire le creature tramite l’odore, anche quando non puoi vederle. Sei in grado di fiutare la presenza di nemici entro nove metri se sei sopravvento, o entro quattro metri e mezzo se sei sottovento. Se ti avvicini a un metro e mezzo dalla fonte dell’odore, riesci a identificarne con precisione la posizione. In alternativa, ne percepisci solo la presenza generica e la direzione. Puoi seguire tracce odorose come se usassi l’abilità Sopravvivenza, ma con un bonus pari al tuo modificatore di Saggezza aumentato di +4. Questo ti consente di inseguire creature anche invisibili o nascoste, purché lascino un odore rilevabile. Non puoi vedere l’invisibile, ma puoi percepire che qualcosa è lì, e in certi casi riuscire ad attaccarlo se sai dove si trova esattamente. L’efficacia dell’Olfatto Acuto dipende dalle condizioni ambientali: il vento può trasportare o disperdere gli odori, e superfici particolarmente pulite o ostili, come la pioggia battente o l’acqua corrente, possono ridurre o annullare la tua capacità di percepire gli odori.',
        null, 1, 1),
       ('Armatura in cuoio borchiato (naturale)', 'EQUIPAGGIAMENTO', 'Armatura', 1, 1, 1),
       ('Scudo in legno', 'EQUIPAGGIAMENTO', 'Scudo', 1, 1, 1),
       ('FromCompendio', 'ALTRO', 'FromCompendio', 1, 1, 1),
       ('Tantoo', 'ARMA', 'Tantoo', null, 1, 1),
       ('Veste del druido', 'EQUIPAGGIAMENTO', 'Veste del druido', null, 1, 1),
       ('Anello Camaleonte', 'EQUIPAGGIAMENTO', 'Anello Camaleonte', null, 1, 1),
       ('Anello Camminare sull''acqua', 'EQUIPAGGIAMENTO', 'Anello Camminare sull''acqua', null, 1, 1),
       ('Sassolino ritornante', 'OGGETTO', 'Sassolino ritornante', 1, 1, 1),
       ('Extra Wild Shape', 'TALENTO', '+2 Utilizzi della forma selvaggia', null, 1, 1),
       ('Forma Selvatica Veloce', 'TALENTO', 'Puoi trasformarti in forma selvaggia con azione veloce', null,
        1, 1),
       ('Senso della natura', 'ABILITA', 'Bonus di +2 alle prove di Conoscenze (natura) e Sopravvivenza',
        null, 1, 1),
       ('Empatia Selvatica', 'ABILITA',
        'Può migliorare l''atteggiamento di un animale (presente entro 9 m, richiede circa 1 min) allo stesso modo in cui una prova di diplomazia superata migliora quello di una creatura senziente (1d20 + liv da druido + mod di Car). Il tipico animale domestico ha un atteggiamento iniziale indifferente, mentre un animale selvatico è solitamente maldisposto. Può tentare anche di influenzare una bestia magica con un punteggio di Int 1 o 2 (come un basilisco o un girallon), ma con una penalità di –4 alla prova.',
        null, 1, 1),
       ('Andatura nel bosco', 'ABILITA',
        'Dal 2° liv: riesce a muoversi attraverso qualsiasi sottobosco non incantato (rovi, sterpi naturali, zone infestate, ecc…) a velocità normale e senza subire danni o altri impedimenti.',
        null, 1, 1),
       ('Passo senza tracce', 'ABILITA',
        'Dal 3° liv: non lascia tracce in ambienti naturali e non può essere inseguito seguendo le tracce.', null, 1,
        1),
       ('Resistenza al richiamo della natura', 'ABILITA',
        'Dal 4° liv: bonus +4 TS contro le capacità magiche dei folletti (driadi, ninfe, spiritelli, …).', null, 1, 1),
       ('Forma Selvatica', 'ABILITA',
        'Dal 5° liv: Metamorfosi in un animale (i DV della nuova forma non possono eccedere il liv da druido) di taglia Piccola o Media (l’effetto dura fino a 1 ora / liv del druido) e di riprendere la sua forma naturale, tot volte al giorno (vedi tabella). È un’azione standard che non provoca attacchi di opportunità.',
        null, 1, 1),
       ('Compagno animale', 'ABILITA',
        'Un druido di 1° liv può scegliere tra: Aquila, Cammello, Cane, Cane da galoppo, Cavallo leggero, Cavallo pesante, Coccodrillo, Falco, Focena, Gufo, Lupo, Pony, Tasso, Topo ferale, Seppia o Squalo medio, Vipera piccola, Vipera media. Può congedarlo dal servizio (o sostituire un animale deceduto) con una cerimonia che richiede 24 ore ininterrotte di preghiera.',
        null, 1, 1),
       ('Attacco', 'ATTACCO', null, null, 1, 1);
;

INSERT INTO stat_value (personaggio_id, stat_id, valore, mod, classe, addestramento)
VALUES (1, 'FOR', '19', null, false, false),
       (1, 'DES', '19', null, false, false),
       (1, 'COS', '20', null, false, false),
       (1, 'INT', '20', null, false, false),
       (1, 'SAG', '24', null, false, false),
       (1, 'CAR', '18', null, false, false),
       (1, 'AB1', '0', 'DES', false, true),
       (1, 'AB2', '0', 'CAR', false, true),
       (1, 'AB3', '0', 'INT', false, false),
       (1, 'AB4', '0', 'DES', false, false),
       (1, 'AB5', '0', 'SAG', false, false),
       (1, 'AB6', '0', 'CAR', false, false),
       (1, 'AB7', '0', 'DES', false, false),
       (1, 'AB8', '0', 'INT', false, false),
       (1, 'AB9', '0', 'COS', false, false),
       (1, 'CO00', '0', 'INT', false, true),
       (1, 'AB11', '0', 'INT', false, true),
       (1, 'AB12', '0', 'CAR', false, false),
       (1, 'AB13', '0', 'INT', false, true),
       (1, 'AB14', '0', 'DES', false, false),
       (1, 'AB15', '0', 'INT', false, false),
       (1, 'AB16', '0', 'SAG', false, false),
       (1, 'AB17', '0', 'CAR', false, false),
       (1, 'AB18', '0', 'CAR', false, false),
       (1, 'AB19', '0', 'DES', false, false),
       (1, 'AB20', '0', 'DES', false, false),
       (1, 'AB21', '0', 'FOR', false, false),
       (1, 'AB22', '0', 'SAG', false, false),
       (1, 'AB23', '0', 'SAG', false, false),
       (1, 'PR00', '0', 'SAG', false, true),
       (1, 'AB25', '0', 'CAR', false, false),
       (1, 'AB26', '0', 'CAR', false, false),
       (1, 'AB27', '0', 'DES', false, true),
       (1, 'AB28', '0', 'FOR', false, false),
       (1, 'AB29', '0', 'INT', false, true),
       (1, 'AB30', '0', 'FOR', false, false),
       (1, 'AB31', '0', 'DES', false, true),
       (1, 'AB32', '0', 'SAG', false, false),
       (1, 'AB33', '0', 'DES', false, false),
       (1, 'AB34', '0', 'CAR', false, true),
       (1, 'AB35', '0', 'INT', false, false),
       (1, 'AB36', '0', NULL, true, true),
       (1, 'PF', '0', NULL, false, false),
       (1, 'PFMAX', '0', NULL, false, false),
       (1, 'PFTEMP', '0', NULL, false, false),
       (1, 'CA', '0', 'DES', false, false),
       (1, 'CAC', '0', 'DES', false, false),
       (1, 'CAS', '0', 'DES', false, false),
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
       (1, 'CO10', '0', 'INT', false, true),
       (1, 'CO11', '0', 'INT', false, true),
       (1, 'PR01', '0', NULL, false, true),
       (1, 'PR02', '0', NULL, false, true),
       (1, 'PR03', '0', NULL, false, true),
       (1, 'PR04', '0', NULL, false, true),
       (1, 'PR05', '0', NULL, false, true),
       (1, 'PR06', '0', NULL, false, true);

INSERT INTO modificatori (id_item, id_stat, valore, always, tipo)
VALUES (3, 'AB12', '-4', true, 'VALORE'),
       (3, 'AB17', '-4', true, 'VALORE'),
       (3, 'AB26', '-4', true, 'VALORE'),
       (3, 'AB32', '+4', true, 'VALORE'),
       (3, 'CO10', '+4', true, 'VALORE'),
       (3, 'AB30', '+4', true, 'VALORE'),
       (3, 'AB21', '+4', true, 'VALORE'),
       (1, 'AB26', '+2', true, 'VALORE'),
       (1, 'AB17', '+2', true, 'VALORE'),
       (1, 'AB23', '+2', true, 'VALORE'),
       (6, 'CAR', '-2', true, 'VALORE'), -- CARISMA
       (4, 'CO10', '+4', true, 'RANK'),    -- CONOSCENZE NATURA
       (4, 'AB9', '+4', true, 'RANK'),   -- CONCENTRAZIONE
       (4, 'AB32', '+4', true, 'RANK'),  -- SOPRAVVIVENZA
       (4, 'AB2', '+1', true, 'RANK'),   -- ADDRESTRARE ANIMALI
       (4, 'AB29', '+4', true, 'RANK'),  -- SAPIENZA MAGICA
       (4, 'AB36', '+1', true, 'RANK'),  -- PARLARE LINGUAGGI
       (4, 'AB16', '+4', true, 'RANK'),  -- GUARIRE
       (4, 'AB21', '+4', true, 'RANK'),  -- NUOTARE
       (4, 'AB5', '+4', true, 'RANK'),   -- ASCOLTARE
       (4, 'CO09', '+2', true, 'RANK'),  -- CONOSCENZA ARCH
       (4, 'AB22', '+4', true, 'RANK'),  -- OSSERVARE
       (5, 'AB16', '+4', true, 'RANK'),  -- GUARIRE
       (5, 'AB21', '+4', true, 'RANK'),  -- NUOTARE
       (6, 'CO09', '+4', true, 'RANK'),  -- CONOSCENZA ARCH
       (6, 'AB9', '+4', true, 'RANK'),   -- CONCENTRAZIONE
       (7, 'AB29', '+4', true, 'RANK'),  -- SAPIENZA MAGICA
       (7, 'AB22', '+4', true, 'RANK'),  -- OSSERVARE
       (8, 'AB5', '+4', true, 'RANK'),   -- ASCOLTARE
       (8, 'CO09', '+4', true, 'RANK'); -- CONOSCENZA ARCH;

INSERT INTO modificatori (id_item, id_stat, valore, always, nota, tipo)
VALUES (1, 'VLT', '+2', false, 'Contro addomentamento e ammaliamento', 'VALORE'),
--        Armatura
       (12, 'AB1', -1, true, null, 'VALORE'),
-- (12, '', -1, true, null, 'VALORE'), ARRAMPICARSI
       (12, 'AB7', -1, true, null, 'VALORE'),
       (12, 'AB19', -1, true, null, 'VALORE'),
       (12, 'AB21', -1, true, null, 'VALORE'),
       (12, 'AB30', -1, true, null, 'VALORE'),
       (12, 'AB28', -1, true, null, 'VALORE'),
       (12, 'CA', +4, true, null, 'CA_ARMOR'),
--        Scudo
       (13, 'AB1', -1, true, null, 'VALORE'),
-- (12, '', -1, true, null, 'VALORE'), ARRAMPICARSI
       (13, 'AB7', -1, true, null, 'VALORE'),
       (13, 'AB19', -1, true, null, 'VALORE'),
       (13, 'AB21', -1, true, null, 'VALORE'),
       (13, 'AB30', -1, true, null, 'VALORE'),
       (13, 'AB28', -1, true, null, 'VALORE'),
       (13, 'CA', +1, true, null, 'CA_SHIELD'),
       (22, 'CO10', +2, true, null, 'VALORE'),
       (22, 'AB32', +2, true, null, 'VALORE'),
       (26, 'TMP', +4, false, 'contro le capacità magiche dei folletti', 'VALORE'),
       (26, 'RFL', +4, false, 'contro le capacità magiche dei folletti', 'VALORE'),
       (26, 'VLT', +4, false, 'contro le capacità magiche dei folletti', 'VALORE');
;

INSERT INTO collegamento (id_item_source, id_item_target)
VALUES (9, 10),
       (10, 11),
       (4, 2),
       (5, 2),
       (6, 2),
       (7, 2),
       (8, 2),
       (14, 15),
       (15, 29),
       (14, 16),
       (14, 17),
       (14, 12),
       (14, 13),
       (14, 18)
;

INSERT INTO item_label (id_item, label, valore)
VALUES (2, 'ABCLASSE', 'AB2,AB5,AB7,AB9,AB12,AB16,AB21,AB22,AB29,AB32,CO10'),
       (29, 'TPC', '@BAB+@FOR'),
       (29, 'TPD', '1d6+@FOR');

INSERT INTO avanzamento (id_item_source, id_item_target, livello)
VALUES (2, 28, 1),
       (2, 22, 1),
       (2, 23, 1),
       (2, 24, 2),
       (2, 25, 3),
       (2, 26, 4),
       (2, 27, 5);

