-- Fix race condition on invoice_number generation.
-- Replace MAX(id)-based approach with an atomic PostgreSQL sequence.
-- The sequence starts after the current max invoice ID to avoid collisions.

DO $$
DECLARE
    max_id BIGINT;
BEGIN
    SELECT COALESCE(MAX(id), 0) + 1 INTO max_id FROM invoices;
    EXECUTE format('CREATE SEQUENCE invoice_number_seq START WITH %s INCREMENT BY 1 NO CYCLE', max_id);
END $$;
