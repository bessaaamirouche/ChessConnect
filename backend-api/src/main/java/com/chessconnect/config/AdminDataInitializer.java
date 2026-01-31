package com.chessconnect.config;

import com.chessconnect.model.User;
import com.chessconnect.model.enums.UserRole;
import com.chessconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(AdminDataInitializer.class);

    @Bean
    CommandLineRunner initAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminEmail = "admin@chessconnect.com";

            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                User admin = new User();
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setFirstName("Admin");
                admin.setLastName("ChessConnect");
                admin.setRole(UserRole.ADMIN);

                userRepository.save(admin);
                log.info("Admin account created: {}", adminEmail);
            } else {
                log.info("Admin account already exists: {}", adminEmail);
            }

            // Second admin account - always update password
            String secondAdminId = "503412850";
            User admin2 = userRepository.findByEmail(secondAdminId).orElse(null);
            if (admin2 == null) {
                admin2 = new User();
                admin2.setEmail(secondAdminId);
                admin2.setFirstName("Admin");
                admin2.setLastName("Principal");
                admin2.setRole(UserRole.ADMIN);
            }
            admin2.setPassword(passwordEncoder.encode("94D723044158a!"));
            userRepository.save(admin2);
            log.info("Second admin account ready: {}", secondAdminId);
        };
    }
}
