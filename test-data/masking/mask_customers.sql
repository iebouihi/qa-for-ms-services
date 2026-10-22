-- Staging refresh, step 2 of 3: anonymise a production SUBSET before it leaves the prod perimeter.
--   1. Extract a consistent subset (e.g. customers active in the last 30 days + their carts),
--      a few GB instead of the full 350 GB, with referential integrity preserved.
--   2. Run this script on the copy, inside the production security zone.
--   3. Load the masked copy into staging.
--
-- Rules: deterministic (same input -> same output, so joins across services still match),
-- format-preserving (an email still looks like an email), irreversible (no lookup table kept).

BEGIN;

UPDATE customer
SET email      = 'user-' || substr(md5(id::text || :'salt'), 1, 12) || '@masked.example',
    first_name = 'First-' || substr(md5(first_name || :'salt'), 1, 6),
    last_name  = 'Last-'  || substr(md5(last_name  || :'salt'), 1, 6);

-- Fail loudly if anything that looks like a real email survived.
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM customer WHERE email NOT LIKE '%@masked.example') THEN
    RAISE EXCEPTION 'Unmasked emails remain, aborting refresh';
  END IF;
END $$;

COMMIT;

-- Usage: psql "$SUBSET_DB_URL" -v salt="$(openssl rand -hex 16)" -f mask_customers.sql
