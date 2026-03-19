CREATE TABLE payments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id     VARCHAR(255)    NOT NULL,
    amount          BIGINT          NOT NULL CHECK (amount > 0),
    currency        VARCHAR(3)      NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    card_last_four  VARCHAR(4),
    idempotency_key VARCHAR(255)    UNIQUE,
    processor_ref   VARCHAR(255),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_merchant_id ON payments (merchant_id);
CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_created_at ON payments (created_at DESC);
