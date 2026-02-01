-- ============================================================================
-- V16: Nettoyage des données de paiement de test
-- Date: 2026-02-01
-- Description: Supprime toutes les données de paiement de test avant passage en prod
-- ============================================================================

-- ATTENTION: Cette migration supprime TOUTES les données de paiement existantes
-- Ne l'exécuter qu'une seule fois lors du passage en production

-- 1. Supprimer les factures (doit être fait avant payments à cause des FK)
DELETE FROM invoices;

-- 2. Supprimer les transactions de crédit wallet
DELETE FROM credit_transactions;

-- 3. Supprimer les paiements
DELETE FROM payments;

-- 4. Supprimer les virements enseignants
DELETE FROM teacher_payouts;

-- 5. Supprimer les abonnements
DELETE FROM subscriptions;

-- 6. Réinitialiser les wallets étudiants (remettre à zéro)
UPDATE student_wallets SET
    balance_cents = 0,
    total_top_ups_cents = 0,
    total_used_cents = 0,
    total_refunded_cents = 0,
    updated_at = NOW();

-- 7. Réinitialiser les soldes enseignants
UPDATE teacher_balances SET
    available_balance_cents = 0,
    pending_balance_cents = 0,
    total_earned_cents = 0,
    total_withdrawn_cents = 0,
    lessons_completed = 0,
    updated_at = NOW();

-- 8. Réinitialiser les champs de paiement sur les lessons (garder l'historique des cours)
UPDATE lessons SET
    refund_percentage = NULL,
    refunded_amount_cents = NULL,
    stripe_refund_id = NULL,
    earnings_credited = FALSE
WHERE refund_percentage IS NOT NULL
   OR refunded_amount_cents IS NOT NULL
   OR stripe_refund_id IS NOT NULL
   OR earnings_credited = TRUE;

-- 9. Réinitialiser le flag "premier cours gratuit" pour tous les utilisateurs
-- Tous les utilisateurs pourront à nouveau bénéficier d'un cours gratuit
UPDATE users SET
    has_used_free_trial = FALSE
WHERE has_used_free_trial = TRUE;

-- 10. Réinitialiser la période d'essai Premium (14 jours sans carte)
-- Tous les utilisateurs pourront recommencer un essai Premium
UPDATE users SET premium_trial_end = NULL WHERE premium_trial_end IS NOT NULL;

-- 11. Réinitialiser les séquences d'ID des tables nettoyées
-- Cela permet aux nouvelles factures de commencer à FAC-2026-000001
ALTER SEQUENCE IF EXISTS invoices_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS payments_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS credit_transactions_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS subscriptions_id_seq RESTART WITH 1;
ALTER SEQUENCE IF EXISTS teacher_payouts_id_seq RESTART WITH 1;

-- Log de confirmation
DO $$
DECLARE
    invoices_deleted INTEGER;
    payments_deleted INTEGER;
    transactions_deleted INTEGER;
    subscriptions_deleted INTEGER;
BEGIN
    -- Compter les enregistrements restants (devrait être 0)
    SELECT COUNT(*) INTO invoices_deleted FROM invoices;
    SELECT COUNT(*) INTO payments_deleted FROM payments;
    SELECT COUNT(*) INTO transactions_deleted FROM credit_transactions;
    SELECT COUNT(*) INTO subscriptions_deleted FROM subscriptions;

    RAISE NOTICE '===========================================';
    RAISE NOTICE 'NETTOYAGE DES DONNEES DE TEST TERMINE';
    RAISE NOTICE 'Date: %', NOW();
    RAISE NOTICE '===========================================';
    RAISE NOTICE 'Factures restantes: %', invoices_deleted;
    RAISE NOTICE 'Paiements restants: %', payments_deleted;
    RAISE NOTICE 'Transactions wallet restantes: %', transactions_deleted;
    RAISE NOTICE 'Abonnements restants: %', subscriptions_deleted;
    RAISE NOTICE '===========================================';
    RAISE NOTICE 'Les wallets et soldes ont ete remis a zero.';
    RAISE NOTICE 'Les sequences d''ID ont ete reinitialises.';
    RAISE NOTICE '===========================================';
END $$;
