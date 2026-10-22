-- Nightly purge of data created by automated tests on shared environments (staging).
-- Tests only create customers with the "test-" prefix and the reserved .test domain
-- (RFC 2606), so synthetic data is unambiguous and never confused with masked data.
-- Run per database; the WHERE clauses below assume the matching schema.

-- customers database
DELETE FROM customer
WHERE email LIKE 'test-%@example.test'
  AND created_at < now() - interval '1 day';

-- carts database (cart_item rows go with ON DELETE CASCADE)
-- DELETE FROM cart WHERE customer_id IN (SELECT customer_id FROM known_customer WHERE email LIKE 'test-%@example.test');
-- DELETE FROM known_customer WHERE email LIKE 'test-%@example.test';
