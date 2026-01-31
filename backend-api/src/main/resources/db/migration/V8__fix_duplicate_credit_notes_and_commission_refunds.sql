-- V8: Fix duplicate credit notes and add missing commission refunds
-- This migration corrects issues with cancelled lessons:
-- 1. Removes duplicate CREDIT_NOTE entries for the same LESSON_INVOICE
-- 2. Updates COMMISSION_INVOICE status to REFUNDED for cancelled lessons
-- 3. Creates missing commission credit notes

-- Step 1: Identify and delete duplicate credit notes (keep only the oldest one per lesson)
WITH duplicate_credit_notes AS (
    SELECT
        cn.id,
        cn.lesson_id,
        cn.original_invoice_id,
        ROW_NUMBER() OVER (
            PARTITION BY cn.lesson_id, cn.original_invoice_id
            ORDER BY cn.created_at ASC
        ) as rn
    FROM invoices cn
    WHERE cn.invoice_type = 'CREDIT_NOTE'
    AND cn.original_invoice_id IN (
        SELECT id FROM invoices WHERE invoice_type = 'LESSON_INVOICE'
    )
)
DELETE FROM invoices
WHERE id IN (
    SELECT id FROM duplicate_credit_notes WHERE rn > 1
);

-- Step 2: Update COMMISSION_INVOICE status to REFUNDED for cancelled lessons
UPDATE invoices
SET status = 'REFUNDED'
WHERE invoice_type = 'COMMISSION_INVOICE'
AND status = 'PAID'
AND lesson_id IN (
    SELECT l.id
    FROM lessons l
    WHERE l.status = 'CANCELLED'
    AND l.refund_percentage = 100
);

-- Update to PARTIALLY_REFUNDED for partial refunds
UPDATE invoices
SET status = 'PARTIALLY_REFUNDED'
WHERE invoice_type = 'COMMISSION_INVOICE'
AND status = 'PAID'
AND lesson_id IN (
    SELECT l.id
    FROM lessons l
    WHERE l.status = 'CANCELLED'
    AND l.refund_percentage > 0
    AND l.refund_percentage < 100
);

-- Step 3: Create missing commission credit notes using a DO block for proper sequencing
DO $$
DECLARE
    rec RECORD;
    next_id BIGINT;
    year_str TEXT;
    new_invoice_number TEXT;
BEGIN
    year_str := EXTRACT(YEAR FROM CURRENT_TIMESTAMP)::TEXT;

    FOR rec IN
        SELECT
            comm.id as commission_invoice_id,
            comm.customer_id as teacher_id,
            comm.lesson_id,
            comm.total_cents as commission_cents,
            l.refund_percentage
        FROM invoices comm
        JOIN lessons l ON comm.lesson_id = l.id
        WHERE comm.invoice_type = 'COMMISSION_INVOICE'
        AND l.status = 'CANCELLED'
        AND l.refund_percentage > 0
        AND NOT EXISTS (
            SELECT 1 FROM invoices cn
            WHERE cn.invoice_type = 'CREDIT_NOTE'
            AND cn.original_invoice_id = comm.id
        )
    LOOP
        -- Get next invoice ID for unique numbering
        SELECT COALESCE(MAX(id), 0) + 1 INTO next_id FROM invoices;
        new_invoice_number := 'AV-' || year_str || '-' || LPAD(next_id::TEXT, 6, '0');

        INSERT INTO invoices (
            invoice_number,
            invoice_type,
            customer_id,
            issuer_id,
            lesson_id,
            original_invoice_id,
            subtotal_cents,
            vat_cents,
            total_cents,
            vat_rate,
            description,
            refund_percentage,
            status,
            issued_at,
            created_at
        ) VALUES (
            new_invoice_number,
            'CREDIT_NOTE',
            rec.teacher_id,
            NULL,
            rec.lesson_id,
            rec.commission_invoice_id,
            -(rec.commission_cents * rec.refund_percentage / 100),
            0,
            -(rec.commission_cents * rec.refund_percentage / 100),
            0,
            CASE
                WHEN rec.refund_percentage = 100 THEN 'Avoir - Annulation commission plateforme'
                ELSE 'Avoir - Remboursement partiel commission (' || rec.refund_percentage || '%)'
            END,
            rec.refund_percentage,
            'REFUNDED',
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        );

        RAISE NOTICE 'Created commission credit note % for lesson %', new_invoice_number, rec.lesson_id;
    END LOOP;
END $$;
