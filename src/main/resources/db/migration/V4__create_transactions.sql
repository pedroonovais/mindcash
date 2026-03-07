CREATE TABLE transactions (
    id              BIGSERIAL      PRIMARY KEY,
    user_id         BIGINT         NOT NULL REFERENCES users(id),
    account_id      BIGINT         NOT NULL REFERENCES accounts(id),
    category_id     BIGINT         REFERENCES categories(id),
    amount          NUMERIC(15,2)  NOT NULL,
    type            VARCHAR(10)    NOT NULL,
    date            DATE           NOT NULL,
    description     VARCHAR(255),
    created_at      TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_user ON transactions (user_id);
CREATE INDEX idx_transactions_account ON transactions (account_id);
CREATE INDEX idx_transactions_category ON transactions (category_id);
CREATE INDEX idx_transactions_date ON transactions (user_id, date);
