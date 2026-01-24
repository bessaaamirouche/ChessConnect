package com.chessconnect.service;

import com.chessconnect.dto.teacher.TeacherBalanceResponse;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.TeacherBalance;
import com.chessconnect.model.User;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.TeacherBalanceRepository;
import com.chessconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeacherBalanceService {

    private static final Logger log = LoggerFactory.getLogger(TeacherBalanceService.class);

    // Prix fixe pour un cours via abonnement (ce que le coach reçoit)
    public static final int SUBSCRIPTION_LESSON_PRICE_CENTS = 1500; // 15€

    private final TeacherBalanceRepository teacherBalanceRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;

    public TeacherBalanceService(
            TeacherBalanceRepository teacherBalanceRepository,
            UserRepository userRepository,
            LessonRepository lessonRepository
    ) {
        this.teacherBalanceRepository = teacherBalanceRepository;
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
    }

    public TeacherBalanceResponse getBalance(Long teacherId) {
        return teacherBalanceRepository.findByTeacherId(teacherId)
                .map(TeacherBalanceResponse::from)
                .orElse(TeacherBalanceResponse.empty(teacherId));
    }

    @Transactional
    public void creditEarningsForCompletedLesson(Lesson lesson) {
        Long teacherId = lesson.getTeacher().getId();

        TeacherBalance balance = teacherBalanceRepository.findByTeacherId(teacherId)
                .orElseGet(() -> createBalanceForTeacher(teacherId));

        int earningsCents;
        if (Boolean.TRUE.equals(lesson.getIsFromSubscription())) {
            // Pour les cours d'abonnement, le coach reçoit un montant fixe
            earningsCents = SUBSCRIPTION_LESSON_PRICE_CENTS;
            log.info("Crediting subscription lesson earnings: {}€ for teacher {}",
                    earningsCents / 100.0, teacherId);
        } else {
            // Pour les cours payés individuellement, on utilise les gains calculés
            Integer teacherEarnings = lesson.getTeacherEarningsCents();
            if (teacherEarnings != null && teacherEarnings > 0) {
                earningsCents = teacherEarnings;
            } else if (lesson.getPriceCents() != null && lesson.getPriceCents() > 0) {
                // Fallback: calculer les gains à partir du prix (85% pour le coach, 15% commission)
                earningsCents = (lesson.getPriceCents() * 85) / 100;
                log.warn("TeacherEarningsCents was null for lesson {}, calculated from price: {}€",
                        lesson.getId(), earningsCents / 100.0);
            } else {
                // Dernier recours: utiliser le tarif horaire du coach
                earningsCents = lesson.getTeacher().getHourlyRateCents() != null
                        ? (lesson.getTeacher().getHourlyRateCents() * 85) / 100
                        : 0;
                log.warn("Using teacher hourly rate as fallback for lesson {}: {}€",
                        lesson.getId(), earningsCents / 100.0);
            }
            log.info("Crediting individual lesson earnings: {}€ for teacher {}",
                    earningsCents / 100.0, teacherId);
        }

        balance.addEarnings(earningsCents);
        teacherBalanceRepository.save(balance);

        log.info("Teacher {} new balance: {}€ (total earned: {}€, lessons: {})",
                teacherId,
                balance.getAvailableBalanceCents() / 100.0,
                balance.getTotalEarnedCents() / 100.0,
                balance.getLessonsCompleted());
    }

    private TeacherBalance createBalanceForTeacher(Long teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        TeacherBalance balance = new TeacherBalance();
        balance.setTeacher(teacher);
        return teacherBalanceRepository.save(balance);
    }

    /**
     * Migration method: credit earnings for all completed lessons that haven't been credited yet.
     * This is useful for lessons that were completed before the earnings tracking was implemented.
     */
    @Transactional
    public int migrateUncreditedLessons() {
        List<Lesson> uncreditedLessons = lessonRepository.findCompletedLessonsNotCredited();
        int count = 0;

        for (Lesson lesson : uncreditedLessons) {
            try {
                creditEarningsForCompletedLesson(lesson);
                lesson.setEarningsCredited(true);
                lessonRepository.save(lesson);
                count++;
                log.info("Migrated earnings for lesson {}", lesson.getId());
            } catch (Exception e) {
                log.error("Failed to migrate earnings for lesson {}: {}", lesson.getId(), e.getMessage());
            }
        }

        log.info("Migration completed: {} lessons credited", count);
        return count;
    }

    /**
     * Recalculate teacher balance from scratch based on completed lessons.
     * Fixes any discrepancies between availableBalanceCents and totalEarnedCents.
     */
    @Transactional
    public TeacherBalanceResponse recalculateBalance(Long teacherId) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        TeacherBalance balance = teacherBalanceRepository.findByTeacherId(teacherId)
                .orElseGet(() -> createBalanceForTeacher(teacherId));

        // Get all completed lessons with earnings credited
        List<Lesson> creditedLessons = lessonRepository.findByTeacherIdAndEarningsCreditedTrue(teacherId);

        // Recalculate total earned
        int totalEarnedCents = 0;
        for (Lesson lesson : creditedLessons) {
            if (Boolean.TRUE.equals(lesson.getIsFromSubscription())) {
                totalEarnedCents += SUBSCRIPTION_LESSON_PRICE_CENTS;
            } else {
                Integer earnings = lesson.getTeacherEarningsCents();
                if (earnings != null && earnings > 0) {
                    totalEarnedCents += earnings;
                } else if (lesson.getPriceCents() != null) {
                    totalEarnedCents += (lesson.getPriceCents() * 85) / 100;
                }
            }
        }

        // Keep existing withdrawn amount
        int totalWithdrawnCents = balance.getTotalWithdrawnCents() != null ? balance.getTotalWithdrawnCents() : 0;

        // Available = earned - withdrawn
        int availableBalanceCents = totalEarnedCents - totalWithdrawnCents;

        // Update balance
        balance.setTotalEarnedCents(totalEarnedCents);
        balance.setAvailableBalanceCents(availableBalanceCents);
        balance.setLessonsCompleted(creditedLessons.size());
        teacherBalanceRepository.save(balance);

        log.info("Recalculated balance for teacher {}: available={}€, totalEarned={}€, withdrawn={}€, lessons={}",
                teacherId,
                availableBalanceCents / 100.0,
                totalEarnedCents / 100.0,
                totalWithdrawnCents / 100.0,
                creditedLessons.size());

        return TeacherBalanceResponse.from(balance);
    }
}
