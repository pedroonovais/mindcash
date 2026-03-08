ALTER TABLE transactions
    ADD COLUMN recurrence_type    VARCHAR(20) NULL,
    ADD COLUMN recurrence_count  INT NULL,
    ADD COLUMN recurrence_next_ym DATE NULL,
    ADD COLUMN recurrence_parent_id BIGINT NULL REFERENCES transactions(id);

CREATE INDEX idx_transactions_recurrence_parent ON transactions (recurrence_parent_id);
CREATE INDEX idx_transactions_recurrence_materialize ON transactions (user_id, recurrence_type, recurrence_next_ym)
    WHERE recurrence_type IS NOT NULL AND recurrence_parent_id IS NULL;
