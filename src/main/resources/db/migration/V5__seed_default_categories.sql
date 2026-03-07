-- Categorias globais (user_id = NULL) disponíveis para todos os usuários

-- Receitas
INSERT INTO categories (user_id, parent_id, name, icon) VALUES (NULL, NULL, 'Receitas', 'trending-up');
INSERT INTO categories (user_id, parent_id, name, icon) VALUES (NULL, (SELECT id FROM categories WHERE name = 'Receitas' AND user_id IS NULL), 'Salário', 'briefcase');
INSERT INTO categories (user_id, parent_id, name, icon) VALUES (NULL, (SELECT id FROM categories WHERE name = 'Receitas' AND user_id IS NULL), 'Freelance', 'code');
INSERT INTO categories (user_id, parent_id, name, icon) VALUES (NULL, (SELECT id FROM categories WHERE name = 'Receitas' AND user_id IS NULL), 'Investimentos', 'bar-chart');
INSERT INTO categories (user_id, parent_id, name, icon) VALUES (NULL, (SELECT id FROM categories WHERE name = 'Receitas' AND user_id IS NULL), 'Outros', 'plus-circle');

-- Despesas
INSERT INTO categories (user_id, parent_id, name, icon) VALUES (NULL, NULL, 'Despesas', 'trending-down');
INSERT INTO categories (user_id, parent_id, name, icon) VALUES (NULL, (SELECT id FROM categories WHERE name = 'Despesas' AND user_id IS NULL), 'Alimentação', 'coffee');
INSERT INTO categories (user_id, parent_id, name, icon) VALUES (NULL, (SELECT id FROM categories WHERE name = 'Despesas' AND user_id IS NULL), 'Transporte', 'truck');
INSERT INTO categories (user_id, parent_id, name, icon) VALUES (NULL, (SELECT id FROM categories WHERE name = 'Despesas' AND user_id IS NULL), 'Moradia', 'home');
INSERT INTO categories (user_id, parent_id, name, icon) VALUES (NULL, (SELECT id FROM categories WHERE name = 'Despesas' AND user_id IS NULL), 'Saúde', 'heart');
INSERT INTO categories (user_id, parent_id, name, icon) VALUES (NULL, (SELECT id FROM categories WHERE name = 'Despesas' AND user_id IS NULL), 'Educação', 'book');
INSERT INTO categories (user_id, parent_id, name, icon) VALUES (NULL, (SELECT id FROM categories WHERE name = 'Despesas' AND user_id IS NULL), 'Lazer', 'smile');
INSERT INTO categories (user_id, parent_id, name, icon) VALUES (NULL, (SELECT id FROM categories WHERE name = 'Despesas' AND user_id IS NULL), 'Compras', 'shopping-bag');
INSERT INTO categories (user_id, parent_id, name, icon) VALUES (NULL, (SELECT id FROM categories WHERE name = 'Despesas' AND user_id IS NULL), 'Contas e Serviços', 'file-text');
INSERT INTO categories (user_id, parent_id, name, icon) VALUES (NULL, (SELECT id FROM categories WHERE name = 'Despesas' AND user_id IS NULL), 'Outros', 'minus-circle');
