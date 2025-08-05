insert into sistema (descrizione)
VALUES ('dnd35e');

insert into mondo (descrizione, sistema_id)
VALUES ('Cico', 1);

insert into utente (id, username, password, name, ruolo)
VALUES (0, 'compendio', '', '', 'ADMIN');

insert into utente (username, password, name, ruolo)
VALUES ('finotto', '', 'Federico Finotto', 'GIOCATORE');