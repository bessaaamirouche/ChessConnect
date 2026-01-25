-- Corriger les factures existantes avec montants NULL
UPDATE invoices
SET subtotal_cents = COALESCE(subtotal_cents, 0),
    vat_cents = COALESCE(vat_cents, 0),
    total_cents = COALESCE(total_cents, 0)
WHERE subtotal_cents IS NULL OR vat_cents IS NULL OR total_cents IS NULL;

-- S'assurer que les colonnes ont une valeur par défaut pour éviter les futurs NULL
ALTER TABLE invoices ALTER COLUMN subtotal_cents SET DEFAULT 0;
ALTER TABLE invoices ALTER COLUMN vat_cents SET DEFAULT 0;
ALTER TABLE invoices ALTER COLUMN total_cents SET DEFAULT 0;
