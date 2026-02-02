package com.chessconnect.service;

import com.chessconnect.model.Lesson;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.LessonStatus;
import com.chessconnect.repository.LessonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class LessonReminderService {

    private static final Logger log = LoggerFactory.getLogger(LessonReminderService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final LessonRepository lessonRepository;
    private final EmailService emailService;
    private final WebPushService webPushService;

    public LessonReminderService(
            LessonRepository lessonRepository,
            EmailService emailService,
            WebPushService webPushService
    ) {
        this.lessonRepository = lessonRepository;
        this.emailService = emailService;
        this.webPushService = webPushService;
    }

    /**
     * Send email reminders for lessons starting in approximately 1 hour.
     * Runs every 15 minutes to catch lessons.
     */
    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void sendLessonReminders() {
        // Find lessons starting in 45-75 minutes that haven't received a reminder
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderWindowStart = now.plusMinutes(45);
        LocalDateTime reminderWindowEnd = now.plusMinutes(75);

        List<Lesson> upcomingLessons = lessonRepository.findLessonsForReminder(
                LessonStatus.CONFIRMED, reminderWindowStart, reminderWindowEnd
        );

        int remindersSent = 0;
        for (Lesson lesson : upcomingLessons) {
            if (Boolean.TRUE.equals(lesson.getReminderSent())) {
                continue;
            }

            // Send reminder to student
            User student = lesson.getStudent();
            User teacher = lesson.getTeacher();

            // Email reminder if enabled
            if (Boolean.TRUE.equals(student.getEmailRemindersEnabled())) {
                sendReminderToStudent(lesson);
                remindersSent++;
            }

            // Push notification reminder (always send if push enabled, as it's less intrusive)
            sendPushReminderToStudent(lesson, student, teacher);

            // Send reminder to teacher if they have reminders enabled
            if (Boolean.TRUE.equals(teacher.getEmailRemindersEnabled())) {
                sendReminderToTeacher(lesson);
                remindersSent++;
            }

            // Push notification reminder for teacher
            sendPushReminderToTeacher(lesson, student, teacher);

            // Mark as sent
            lesson.setReminderSent(true);
            lessonRepository.save(lesson);
        }

        if (remindersSent > 0) {
            log.info("Sent {} lesson reminders", remindersSent);
        }
    }

    private void sendReminderToStudent(Lesson lesson) {
        User student = lesson.getStudent();
        User teacher = lesson.getTeacher();

        String lessonDate = lesson.getScheduledAt().format(DATE_FORMATTER);
        String lessonTime = lesson.getScheduledAt().format(TIME_FORMATTER);
        String teacherName = teacher.getFirstName() + " " + teacher.getLastName();
        String meetingLink = lesson.getZoomLink() != null ? lesson.getZoomLink() : "";

        emailService.sendLessonReminderEmail(
                student.getEmail(),
                student.getFirstName(),
                teacherName,
                lessonDate,
                lessonTime,
                meetingLink
        );

        log.debug("Sent reminder to student {} for lesson {}", student.getEmail(), lesson.getId());
    }

    private void sendReminderToTeacher(Lesson lesson) {
        User student = lesson.getStudent();
        User teacher = lesson.getTeacher();

        String lessonDate = lesson.getScheduledAt().format(DATE_FORMATTER);
        String lessonTime = lesson.getScheduledAt().format(TIME_FORMATTER);
        String studentName = student.getFirstName() + " " + student.getLastName();
        String meetingLink = lesson.getZoomLink() != null ? lesson.getZoomLink() : "";

        emailService.sendLessonReminderEmail(
                teacher.getEmail(),
                teacher.getFirstName(),
                studentName,
                lessonDate,
                lessonTime,
                meetingLink
        );

        log.debug("Sent reminder to teacher {} for lesson {}", teacher.getEmail(), lesson.getId());
    }

    private void sendPushReminderToStudent(Lesson lesson, User student, User teacher) {
        try {
            String teacherName = teacher.getFirstName() + " " + teacher.getLastName();
            String lessonTime = lesson.getScheduledAt().format(TIME_FORMATTER);

            webPushService.sendToUser(
                    student.getId(),
                    "Rappel : Cours dans 1 heure",
                    String.format("Votre cours avec %s commence a %s", teacherName, lessonTime),
                    "/lessons",
                    "/assets/icons/icon-192x192.png",
                    true // Force send even if connected via SSE (reminder is important)
            );
        } catch (Exception e) {
            log.warn("Failed to send push reminder to student {}: {}", student.getId(), e.getMessage());
        }
    }

    private void sendPushReminderToTeacher(Lesson lesson, User student, User teacher) {
        try {
            String studentName = student.getFirstName() + " " + student.getLastName();
            String lessonTime = lesson.getScheduledAt().format(TIME_FORMATTER);

            webPushService.sendToUser(
                    teacher.getId(),
                    "Rappel : Cours dans 1 heure",
                    String.format("Votre cours avec %s commence a %s", studentName, lessonTime),
                    "/lessons",
                    "/assets/icons/icon-192x192.png",
                    true // Force send even if connected via SSE (reminder is important)
            );
        } catch (Exception e) {
            log.warn("Failed to send push reminder to teacher {}: {}", teacher.getId(), e.getMessage());
        }
    }
}
