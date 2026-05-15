-- Usuários iniciais
INSERT INTO users (name, balance)
VALUES ('João Silva', 5000.00),
       ('Maria Souza', 3000.00)
ON CONFLICT DO NOTHING;

-- Estoque do caixa: 25 notas de cada denominação
INSERT INTO atm_cash_inventory (denomination, quantity)
VALUES (2,  25),
       (5,  25),
       (10, 25),
       (50, 25)
ON CONFLICT (denomination) DO NOTHING;
