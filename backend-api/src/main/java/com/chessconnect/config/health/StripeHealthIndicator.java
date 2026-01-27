package com.chessconnect.config.health;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Balance;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class StripeHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            // Simple check - verify Stripe API is reachable
            if (Stripe.apiKey == null || Stripe.apiKey.isEmpty()) {
                return Health.down()
                        .withDetail("error", "Stripe API key not configured")
                        .build();
            }

            // Try to retrieve balance to verify connectivity
            Balance.retrieve();

            return Health.up()
                    .withDetail("service", "Stripe API")
                    .withDetail("status", "connected")
                    .build();

        } catch (StripeException e) {
            return Health.down()
                    .withDetail("service", "Stripe API")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
