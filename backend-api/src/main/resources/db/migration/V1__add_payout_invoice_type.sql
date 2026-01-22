-- Migration to add PAYOUT_INVOICE type to the invoices table check constraint

-- Drop the existing constraint
ALTER TABLE invoices DROP CONSTRAINT IF EXISTS invoices_invoice_type_check;

-- Add the updated constraint with PAYOUT_INVOICE
ALTER TABLE invoices ADD CONSTRAINT invoices_invoice_type_check
    CHECK (invoice_type IN ('LESSON_INVOICE', 'COMMISSION_INVOICE', 'PAYOUT_INVOICE'));
