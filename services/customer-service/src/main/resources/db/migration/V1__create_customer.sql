CREATE TABLE customer (
    id          UUID PRIMARY KEY,
    email       VARCHAR(255) NOT NULL,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Case-insensitive uniqueness: a real PostgreSQL feature HSQLDB would not have caught.
CREATE UNIQUE INDEX ux_customer_email ON customer (LOWER(email));
