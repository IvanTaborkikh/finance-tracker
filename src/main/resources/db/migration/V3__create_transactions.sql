CREATE TABLE transactions (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id          BIGINT         NOT NULL REFERENCES users (id),
    category_id      BIGINT         NOT NULL REFERENCES categories (id),
    amount           NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    currency         VARCHAR(3)     NOT NULL DEFAULT 'PLN' CHECK (currency = 'PLN'),
    type             VARCHAR(20)    NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    description      VARCHAR(255),
    transaction_date DATE           NOT NULL,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- Майже кожен запит: "транзакції цього юзера за період" (список, звіти, бюджети)
CREATE INDEX idx_transactions_user_date ON transactions (user_id, transaction_date);

-- Фільтр за категорією + перевірка FK при видаленні категорії
CREATE INDEX idx_transactions_category_id ON transactions (category_id);
