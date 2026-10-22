CREATE TABLE product (
    id           UUID PRIMARY KEY,
    sku          VARCHAR(64)    NOT NULL UNIQUE,
    name         VARCHAR(200)   NOT NULL,
    description  VARCHAR(2000),
    price        NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    currency     VARCHAR(3)     NOT NULL,
    active       BOOLEAN        NOT NULL DEFAULT TRUE
);

CREATE INDEX ix_product_name ON product (LOWER(name));
