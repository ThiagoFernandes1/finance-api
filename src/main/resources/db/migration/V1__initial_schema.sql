CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(180) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL
);

CREATE UNIQUE INDEX ux_users_email ON users (LOWER(email));

CREATE TABLE categories (
    id      UUID        PRIMARY KEY,
    name    VARCHAR(80) NOT NULL,
    type    VARCHAR(10) NOT NULL,
    color   VARCHAR(7),
    user_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_categories_type CHECK (type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT ux_categories_user_name UNIQUE (user_id, name)
);

CREATE INDEX ix_categories_user ON categories (user_id);

CREATE TABLE accounts (
    id              UUID           PRIMARY KEY,
    name            VARCHAR(80)    NOT NULL,
    institution     VARCHAR(60),
    initial_balance NUMERIC(19, 2) NOT NULL,
    user_id         UUID           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ux_accounts_user_name UNIQUE (user_id, name)
);

CREATE INDEX ix_accounts_user ON accounts (user_id);

CREATE TABLE transactions (
    id          UUID           PRIMARY KEY,
    description VARCHAR(160)   NOT NULL,
    amount      NUMERIC(19, 2) NOT NULL,
    type        VARCHAR(10)    NOT NULL,
    occurred_on DATE           NOT NULL,
    account_id  UUID           NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    category_id UUID           NOT NULL REFERENCES categories (id),
    user_id     UUID           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at  TIMESTAMP      NOT NULL,
    CONSTRAINT ck_transactions_type CHECK (type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT ck_transactions_amount_positive CHECK (amount > 0)
);

-- Cobre o filtro mais frequente da aplicacao: extrato do usuario por periodo.
CREATE INDEX ix_transactions_user_date ON transactions (user_id, occurred_on DESC);
CREATE INDEX ix_transactions_account ON transactions (account_id);
CREATE INDEX ix_transactions_category ON transactions (category_id);

CREATE TABLE budgets (
    id              UUID           PRIMARY KEY,
    limit_amount    NUMERIC(19, 2) NOT NULL,
    reference_month DATE           NOT NULL,
    category_id     UUID           NOT NULL REFERENCES categories (id) ON DELETE CASCADE,
    user_id         UUID           NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_budgets_limit_positive CHECK (limit_amount > 0),
    CONSTRAINT ux_budgets_user_category_month UNIQUE (user_id, category_id, reference_month)
);

CREATE INDEX ix_budgets_user_month ON budgets (user_id, reference_month);
