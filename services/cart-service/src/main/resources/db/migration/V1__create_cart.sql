-- Local projection of customers, fed by the customer.registered event (eventual consistency).
CREATE TABLE known_customer (
    customer_id    UUID PRIMARY KEY,
    email          VARCHAR(255) NOT NULL,
    registered_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE cart (
    id           UUID PRIMARY KEY,
    customer_id  UUID           NOT NULL REFERENCES known_customer (customer_id),
    currency     VARCHAR(3)     NOT NULL,
    total        NUMERIC(12, 2) NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE cart_item (
    id            UUID PRIMARY KEY,
    cart_id       UUID           NOT NULL REFERENCES cart (id) ON DELETE CASCADE,
    product_id    UUID           NOT NULL,
    product_name  VARCHAR(200)   NOT NULL,
    unit_price    NUMERIC(12, 2) NOT NULL,
    quantity      INTEGER        NOT NULL CHECK (quantity BETWEEN 1 AND 99),
    line_total    NUMERIC(12, 2) NOT NULL
);

CREATE INDEX ix_cart_customer ON cart (customer_id);
CREATE INDEX ix_cart_item_cart ON cart_item (cart_id);
