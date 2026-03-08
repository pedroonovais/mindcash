-- Permite entrada inicial (posição sem conta de origem)
ALTER TABLE investments ALTER COLUMN source_account_id DROP NOT NULL;

-- Valor atual atualizado diariamente pelo job
ALTER TABLE investments ADD COLUMN current_value NUMERIC(15,2);
UPDATE investments SET current_value = amount WHERE current_value IS NULL;
ALTER TABLE investments ALTER COLUMN current_value SET NOT NULL;

-- Ativo opcional (ticker/nome do papel ou fundo)
ALTER TABLE investments ADD COLUMN asset_name VARCHAR(100);
