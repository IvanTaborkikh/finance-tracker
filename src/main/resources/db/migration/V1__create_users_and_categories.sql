
CREATE TABLE users(
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    display_name    VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE categories(
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT REFERENCES users (id),
    name            VARCHAR(50) NOT NULL,
    type            VARCHAR(20) NOT NULL CHECK ( type IN ('INCOME', 'EXPENSE')),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_categories_user_name_type
        UNIQUE NULLS NOT DISTINCT (user_id, name, type)
);

-- users can only see their own categories