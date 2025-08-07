INSERT INTO ITEMS (nome, tipo, descrizione, personaggio_id, id_sistema, id_mondo)
VALUES ('Fucile Ammazzadivinita', 'ARMA', 'Fucile Ammazzadivinita', null, 1, 1),
       ('Maledizione del fucile Ammazzadivinita', 'MALEDIZIONE', 'Maledizione del fucile Ammazzadivinita',
        null, 1, 1),
       ('Olfatto Acuto', 'ABILITA',
        'Olfatto Acuto ti permette di percepire le creature tramite l’odore, anche quando non puoi vederle. Sei in grado di fiutare la presenza di nemici entro nove metri se sei sopravvento, o entro quattro metri e mezzo se sei sottovento. Se ti avvicini a un metro e mezzo dalla fonte dell’odore, riesci a identificarne con precisione la posizione. In alternativa, ne percepisci solo la presenza generica e la direzione. Puoi seguire tracce odorose come se usassi l’abilità Sopravvivenza, ma con un bonus pari al tuo modificatore di Saggezza aumentato di +4. Questo ti consente di inseguire creature anche invisibili o nascoste, purché lascino un odore rilevabile. Non puoi vedere l’invisibile, ma puoi percepire che qualcosa è lì, e in certi casi riuscire ad attaccarlo se sai dove si trova esattamente. L’efficacia dell’Olfatto Acuto dipende dalle condizioni ambientali: il vento può trasportare o disperdere gli odori, e superfici particolarmente pulite o ostili, come la pioggia battente o l’acqua corrente, possono ridurre o annullare la tua capacità di percepire gli odori.',
        null, 1, 1),
       ('Armatura in cuoio borchiato (naturale)', 'EQUIPAGGIAMENTO', 'Armatura', null, 1, 1),
       ('Scudo in legno', 'EQUIPAGGIAMENTO', 'Scudo', null, 1, 1),
       ('Tantoo', 'ARMA', 'Tantoo', null, 1, 1),
       ('Veste del druido', 'EQUIPAGGIAMENTO', 'Veste del druido', null, 1, 1),
       ('Anello Camaleonte', 'EQUIPAGGIAMENTO', 'Anello Camaleonte', null, 1, 1),
       ('Anello Camminare sull''acqua', 'EQUIPAGGIAMENTO', 'Anello Camminare sull''acqua', null, 1, 1),
       ('Sassolino ritornante', 'OGGETTO', 'Sassolino ritornante', null, 1, 1),
       ('Extra Wild Shape', 'TALENTO', '+2 Utilizzi della forma selvaggia', null, 1, 1),
       ('Forma Selvatica Veloce', 'TALENTO', 'Puoi trasformarti in forma selvaggia con azione veloce', null,
        1, 1),
       ('Attacco', 'ATTACCO', 'ATTACCO TANTOO', null, 1, 1);

INSERT INTO item_label (id_item, label, valore)
VALUES
    ((SELECT id FROM items WHERE descrizione = 'ATTACCO TANTOO'), 'TPC', '@BAB+@FOR'),
    ((SELECT id FROM items WHERE descrizione = 'ATTACCO TANTOO'), 'TPD', '1d6+@FOR');


INSERT INTO modificatori (id_item, id_stat, valore, always, nota, tipo)
VALUES
    -- armatura
    ((select id from items where nome = 'Armatura in cuoio borchiato (naturale)'), 'AB1', -1, true, null, 'VALORE'),
    ((select id from items where nome = 'Armatura in cuoio borchiato (naturale)'), 'AB7', -1, true, null, 'VALORE'),
    ((select id from items where nome = 'Armatura in cuoio borchiato (naturale)'), 'AB19', -1, true, null, 'VALORE'),
    ((select id from items where nome = 'Armatura in cuoio borchiato (naturale)'), 'AB21', -1, true, null, 'VALORE'),
    ((select id from items where nome = 'Armatura in cuoio borchiato (naturale)'), 'AB30', -1, true, null, 'VALORE'),
    ((select id from items where nome = 'Armatura in cuoio borchiato (naturale)'), 'AB28', -1, true, null, 'VALORE'),
    ((select id from items where nome = 'Armatura in cuoio borchiato (naturale)'), 'CA', +4, true, null, 'CA_ARMOR'),
--        Scudo
    ((select id from items where nome = 'Scudo in legno'), 'AB1', -1, true, null, 'VALORE'),
    ((select id from items where nome = 'Scudo in legno'), 'AB7', -1, true, null, 'VALORE'),
    ((select id from items where nome = 'Scudo in legno'), 'AB19', -1, true, null, 'VALORE'),
    ((select id from items where nome = 'Scudo in legno'), 'AB21', -1, true, null, 'VALORE'),
    ((select id from items where nome = 'Scudo in legno'), 'AB30', -1, true, null, 'VALORE'),
    ((select id from items where nome = 'Scudo in legno'), 'AB28', -1, true, null, 'VALORE'),
    ((select id from items where nome = 'Scudo in legno'), 'CA', +4, true, null, 'CA_SHIELD')

;

INSERT INTO collegamento (id_item_source, id_item_target)
VALUES
    ((SELECT id FROM items where nome = 'Fucile Ammazzadivinita'), (SELECT id FROM items where nome = 'Maledizione del fucile Ammazzadivinita')),
    ((SELECT id FROM items where nome = 'Maledizione del fucile Ammazzadivinita'), (SELECT id FROM items where nome = 'Olfatto Acuto'))

;

INSERT INTO collegamento (id_item_source, id_item_target)
VALUES
    ((SELECT id FROM items where nome = 'Tantoo'), (SELECT id FROM items where descrizione = 'ATTACCO TANTOO'))

;

INSERT INTO modificatori (id_item, id_stat, valore, always, nota, tipo)
VALUES
    ((select id from items where nome = 'Extra Wild Shape'), 'NWS', '+2', true, null, 'VALORE'),
    ((select id from items where nome = 'Veste del druido'), 'NWS', '+1', true, null, 'VALORE');

INSERT INTO items (nome, tipo, descrizione, personaggio_id, id_sistema, id_mondo) VALUES
('Resistenza Fisica', 'TALENTO', 'Il personaggio ottiene un bonus di +4 alle prove e ai tiri salvezza elencati di seguito. Il personaggio può inoltre dormire con indosso armature medie senza svegliarsi affaticato.', null, 1, null);

INSERT INTO modificatori (id_item, id_stat, valore, always, nota, tipo)
VALUES
    -- Resistenza fisica
    ((select id from items where nome = 'Resistenza Fisica'), 'AB21', '+4', false, 'per evitare i danni non letali', 'VALORE'), --NUOTARE
    ((select id from items where nome = 'Resistenza Fisica'), 'TMP', '+4', false, 'per non smettere di correre', 'VALORE'), -- COSTITUZIONE
    ((select id from items where nome = 'Resistenza Fisica'), 'TMP', '+4', false, 'danni non letali causati da marce forzate', 'VALORE'),
    ((select id from items where nome = 'Resistenza Fisica'), 'TMP', '+4', false, 'per trattenere il fiato', 'VALORE'),
    ((select id from items where nome = 'Resistenza Fisica'), 'TMP', '+4', false, 'per resistere alla fame o alla sete', 'VALORE'),
    ((select id from items where nome = 'Resistenza Fisica'), 'TMP', '+4', false, 'danni non letali causati da temperatura', 'VALORE'),
    ((select id from items where nome = 'Resistenza Fisica'), 'TMP', '+4', false, 'per evitare i danni causati da soffocamento', 'VALORE')
;


