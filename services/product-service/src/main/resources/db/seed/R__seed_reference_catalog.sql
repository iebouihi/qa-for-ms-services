-- Reference catalog for non-production environments: test data as code, versioned in Git,
-- loaded only with the "seed" profile (never in production). Idempotent: re-runs when edited.
-- Fixed ids make the catalog identical in CI, ephemeral environments and staging.
INSERT INTO product (id, sku, name, description, price, currency, active) VALUES
  ('00000000-0000-0000-0000-000000000001', 'SKU-HEADPHONES', 'Wireless Headphones', 'Over-ear, noise cancelling, 30h battery', 149.90, 'EUR', TRUE),
  ('00000000-0000-0000-0000-000000000002', 'SKU-KEYBOARD',   'Mechanical Keyboard', 'Tenkeyless, hot-swappable switches',       89.00, 'EUR', TRUE),
  ('00000000-0000-0000-0000-000000000003', 'SKU-MOUSE',      'Ergonomic Mouse',     'Vertical design, Bluetooth',                45.50, 'EUR', TRUE),
  ('00000000-0000-0000-0000-000000000004', 'SKU-MONITOR',    '27" 4K Monitor',      'IPS panel, USB-C 90W',                     399.00, 'EUR', TRUE),
  ('00000000-0000-0000-0000-000000000005', 'SKU-WEBCAM',     'HD Webcam',           '1080p, privacy shutter',                    59.99, 'EUR', TRUE),
  ('00000000-0000-0000-0000-000000000006', 'SKU-DOCK',       'USB-C Dock',          'Discontinued model',                       129.00, 'EUR', FALSE)
ON CONFLICT (id) DO UPDATE SET
  sku = EXCLUDED.sku, name = EXCLUDED.name, description = EXCLUDED.description,
  price = EXCLUDED.price, currency = EXCLUDED.currency, active = EXCLUDED.active;
