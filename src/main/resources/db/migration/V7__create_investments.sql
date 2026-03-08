-- Tabela de investimentos (aplicações com tipo e rentabilidade)
CREATE TABLE investments (
    id                      BIGSERIAL      PRIMARY KEY,
    user_id                 BIGINT         NOT NULL REFERENCES users(id),
    source_account_id       BIGINT         NOT NULL REFERENCES accounts(id),
    destination_account_id  BIGINT         NOT NULL REFERENCES accounts(id),
    amount                  NUMERIC(15,2)  NOT NULL,
    date                    DATE           NOT NULL,
    investment_type         VARCHAR(30)    NOT NULL,
    rentability_value       NUMERIC(10,2)  NOT NULL,
    rentability_kind        VARCHAR(20)    NOT NULL,
    description             VARCHAR(255),
    created_at              TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_investments_user ON investments (user_id);
CREATE INDEX idx_investments_date ON investments (user_id, date);
CREATE INDEX idx_investments_destination ON investments (destination_account_id);

-- Vincula transações geradas pela aplicação ao investimento (para reversão na exclusão)
ALTER TABLE transactions ADD COLUMN investment_id BIGINT REFERENCES investments(id);
CREATE INDEX idx_transactions_investment ON transactions (investment_id);
