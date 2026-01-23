package com.chessconnect.config;

import com.chessconnect.model.User;
import com.chessconnect.model.enums.UserRole;
import com.chessconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
public class TestDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(TestDataInitializer.class);

    @Bean
    @Order(2)
    CommandLineRunner initTestData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Check if test data already exists
            if (userRepository.findByEmail("laurent.mercier@gmail.com").isPresent()) {
                log.info("Test data already exists, skipping initialization");
                return;
            }

            String encodedPassword = passwordEncoder.encode("admin123");

            // Create 10 teachers with French and Maghrebi names
            List<User> teachers = List.of(
                createTeacher("Laurent", "Mercier", "laurent.mercier@gmail.com", encodedPassword,
                    4500, "Champion regional 2019. 15 ans d'experience dans l'enseignement des echecs.", "FR",
                    "/api/uploads/avatars/teacher-laurent.jpg"),
                createTeacher("Karim", "Benali", "karim.benali@gmail.com", encodedPassword,
                    5500, "Maitre FIDE. Ancien entraineur du club de Marseille.", "FR,AR",
                    "/api/uploads/avatars/teacher-karim.jpg"),
                createTeacher("Sophie", "Martin", "sophie.martin@gmail.com", encodedPassword,
                    4000, "Passionnee depuis l'enfance. Diplome d'entraineur FFE.", "FR,EN",
                    "/api/uploads/avatars/teacher-sophie.jpg"),
                createTeacher("Youssef", "Alami", "youssef.alami@gmail.com", encodedPassword,
                    6000, "Grand Maitre International. Coaching personnalise.", "FR,AR,EN",
                    "/api/uploads/avatars/teacher-youssef.jpg"),
                createTeacher("Claire", "Dubois", "claire.dubois@gmail.com", encodedPassword,
                    3500, "Specialisee dans l'initiation des enfants de 6 a 12 ans.", "FR",
                    "/api/uploads/avatars/teacher-claire.jpg"),
                createTeacher("Mehdi", "Bouzid", "mehdi.bouzid@gmail.com", encodedPassword,
                    5000, "Expert en ouvertures siciliennes et indiennes.", "FR,AR",
                    "/api/uploads/avatars/teacher-mehdi.jpg"),
                createTeacher("Philippe", "Leroy", "philippe.leroy@gmail.com", encodedPassword,
                    4800, "Retraite passionne. Patience et pedagogie.", "FR",
                    "/api/uploads/avatars/teacher-philippe.jpg"),
                createTeacher("Fatima", "Kaddouri", "fatima.kaddouri@gmail.com", encodedPassword,
                    4200, "Championne feminine du Maroc 2018.", "FR,AR",
                    "/api/uploads/avatars/teacher-fatima.jpg"),
                createTeacher("Nicolas", "Bernard", "nicolas.bernard@gmail.com", encodedPassword,
                    5200, "Maitre International. Preparation mentale et strategique.", "FR,EN",
                    "/api/uploads/avatars/teacher-nicolas.jpg"),
                createTeacher("Amina", "Cherif", "amina.cherif@gmail.com", encodedPassword,
                    4000, "Cours adaptes aux femmes souhaitant progresser.", "FR,AR,EN",
                    "/api/uploads/avatars/teacher-amina.jpg")
            );

            userRepository.saveAll(teachers);
            log.info("Created {} test teachers", teachers.size());

            // Create 3 students
            List<User> students = List.of(
                createStudent("Marc", "Petit", "marc.petit@gmail.com", encodedPassword),
                createStudent("Sarah", "Cohen", "sarah.cohen@gmail.com", encodedPassword),
                createStudent("Omar", "Slimani", "omar.slimani@gmail.com", encodedPassword)
            );

            userRepository.saveAll(students);
            log.info("Created {} test students", students.size());
        };
    }

    private User createTeacher(String firstName, String lastName, String email, String password,
                                int hourlyRateCents, String bio, String languages, String avatarUrl) {
        User teacher = new User();
        teacher.setFirstName(firstName);
        teacher.setLastName(lastName);
        teacher.setEmail(email);
        teacher.setPassword(password);
        teacher.setRole(UserRole.TEACHER);
        teacher.setHourlyRateCents(hourlyRateCents);
        teacher.setAcceptsSubscription(true);
        teacher.setBio(bio);
        teacher.setLanguages(languages);
        teacher.setAvatarUrl(avatarUrl);
        teacher.setIsSuspended(false);
        return teacher;
    }

    private User createStudent(String firstName, String lastName, String email, String password) {
        User student = new User();
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setEmail(email);
        student.setPassword(password);
        student.setRole(UserRole.STUDENT);
        student.setIsSuspended(false);
        return student;
    }
}
