CREATE TABLE categories (
    id         BIGSERIAL     PRIMARY KEY,
    user_id    BIGINT        REFERENCES users(id),
    parent_id  BIGINT        REFERENCES categories(id),
    name       VARCHAR(80)   NOT NULL,
    icon       VARCHAR(50),
    created_at TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_categories_user ON categories (user_id);
CREATE INDEX idx_categories_parent ON categories (parent_id);
