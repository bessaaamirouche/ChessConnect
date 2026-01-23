package com.chessconnect.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class SchemaMigrationInitializer {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigrationInitializer.class);

    @Bean
    @Order(0) // Run before other initializers
    CommandLineRunner migrateSchema(JdbcTemplate jdbcTemplate) {
        return args -> {
            log.info("Running schema migrations...");

            // Remove NOT NULL constraint from monthly_quota if it exists
            try {
                jdbcTemplate.execute(
                    "ALTER TABLE subscriptions ALTER COLUMN monthly_quota DROP NOT NULL"
                );
                log.info("Removed NOT NULL constraint from monthly_quota");
            } catch (Exception e) {
                // Column might not exist or constraint already removed
                log.debug("monthly_quota migration skipped: {}", e.getMessage());
            }

            // Remove NOT NULL constraint from lessons_remaining if it exists
            try {
                jdbcTemplate.execute(
                    "ALTER TABLE subscriptions ALTER COLUMN lessons_remaining DROP NOT NULL"
                );
                log.info("Removed NOT NULL constraint from lessons_remaining");
            } catch (Exception e) {
                // Column might not exist or constraint already removed
                log.debug("lessons_remaining migration skipped: {}", e.getMessage());
            }

            log.info("Schema migrations completed");
        };
    }
}
