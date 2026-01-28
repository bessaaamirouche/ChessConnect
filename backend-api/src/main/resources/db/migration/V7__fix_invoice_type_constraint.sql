-- Fix invoice_type check constraint to include all required types
ALTER TABLE invoices DROP CONSTRAINT IF EXISTS invoices_invoice_type_check;
ALTER TABLE invoices ADD CONSTRAINT invoices_invoice_type_check
CHECK (invoice_type IN ('LESSON_INVOICE', 'COMMISSION_INVOICE', 'PAYOUT_INVOICE', 'CREDIT_NOTE', 'SUBSCRIPTION', 'CREDIT_TOPUP'));
