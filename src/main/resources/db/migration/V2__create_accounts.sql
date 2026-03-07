CREATE TABLE accounts (
    id         BIGSERIAL      PRIMARY KEY,
    user_id    BIGINT         NOT NULL REFERENCES users(id),
    name       VARCHAR(100)   NOT NULL,
    type       VARCHAR(30)    NOT NULL,
    currency   VARCHAR(3)     NOT NULL DEFAULT 'BRL',
    balance    NUMERIC(15,2)  NOT NULL DEFAULT 0,
    created_at TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at TIMESTAMP      NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_accounts_user ON accounts (user_id);
