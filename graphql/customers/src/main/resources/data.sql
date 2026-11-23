insert into address(id, city) values (4, 'Bucuresti');
insert into customer(id, name, email, invoicing_address_id) values (1, 'Ana', 'ana@shop.ro', 4), (2, 'Bogdan', 'bogdan@shop.ro', null);
insert into address(id, city, customer_id) values (1, 'Cluj', 1), (2, 'Iasi', 1), (3, 'Brasov', 2);
