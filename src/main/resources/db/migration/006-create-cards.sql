CREATE TABLE IF NOT EXISTS cards (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    encrypted_number VARCHAR(255) NOT NULL UNIQUE,
    owner_id BIGINT NOT NULL,
    expiry_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    balance NUMERIC(19, 2) NOT NULL DEFAULT 0.00,

    CONSTRAINT fk_cards_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_cards_owner_id ON cards (owner_id);
CREATE INDEX IF NOT EXISTS idx_cards_owner_id ON cards (encrypted_number);