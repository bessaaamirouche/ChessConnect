package com.chessconnect.config.health;

import com.chessconnect.repository.UserRepository;
import com.chessconnect.repository.LessonRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class ApplicationHealthIndicator implements HealthIndicator {

    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;

    public ApplicationHealthIndicator(UserRepository userRepository, LessonRepository lessonRepository) {
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
    }

    @Override
    public Health health() {
        try {
            // Check database connectivity and basic data
            long userCount = userRepository.count();
            long lessonCount = lessonRepository.count();

            return Health.up()
                    .withDetail("service", "ChessConnect API")
                    .withDetail("users", userCount)
                    .withDetail("lessons", lessonCount)
                    .withDetail("status", "operational")
                    .build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("service", "ChessConnect API")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
