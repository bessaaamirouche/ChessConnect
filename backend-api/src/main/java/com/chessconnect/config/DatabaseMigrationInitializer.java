package com.chessconnect.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(1) // Run early
public class DatabaseMigrationInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrationInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigrationInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        updateInvoiceTypeConstraint();
    }

    private void updateInvoiceTypeConstraint() {
        try {
            // Check if constraint needs update by trying to see if PAYOUT_INVOICE is allowed
            // Drop and recreate the constraint to include PAYOUT_INVOICE
            logger.info("Updating invoices_invoice_type_check constraint to include PAYOUT_INVOICE...");

            jdbcTemplate.execute("ALTER TABLE invoices DROP CONSTRAINT IF EXISTS invoices_invoice_type_check");
            jdbcTemplate.execute(
                "ALTER TABLE invoices ADD CONSTRAINT invoices_invoice_type_check " +
                "CHECK (invoice_type IN ('LESSON_INVOICE', 'COMMISSION_INVOICE', 'PAYOUT_INVOICE'))"
            );

            logger.info("Invoice type constraint updated successfully");
        } catch (Exception e) {
            logger.warn("Could not update invoice type constraint (may already be correct): {}", e.getMessage());
        }
    }
}
