-- Same masking on cart-service's projection, with the SAME salt as mask_customers.sql:
-- deterministic masking keeps customer ids and emails consistent across services.
BEGIN;

UPDATE known_customer
SET email = 'user-' || substr(md5(customer_id::text || :'salt'), 1, 12) || '@masked.example';

COMMIT;
