-- Add denormalized name/email fields to invoices for legal data retention (10 years)
-- When user accounts are deleted, these fields preserve the identity on invoices

ALTER TABLE invoices ADD COLUMN customer_name VARCHAR(255);
ALTER TABLE invoices ADD COLUMN customer_email VARCHAR(255);
ALTER TABLE invoices ADD COLUMN issuer_name VARCHAR(255);
ALTER TABLE invoices ADD COLUMN issuer_email VARCHAR(255);

-- Allow customer_id to be nullable (for when user accounts are deleted)
ALTER TABLE invoices ALTER COLUMN customer_id DROP NOT NULL;

-- Populate denormalized fields from existing user data
UPDATE invoices i
SET customer_name = u.first_name || ' ' || u.last_name,
    customer_email = u.email
FROM users u
WHERE i.customer_id = u.id;

UPDATE invoices i
SET issuer_name = u.first_name || ' ' || u.last_name,
    issuer_email = u.email
FROM users u
WHERE i.issuer_id = u.id;
