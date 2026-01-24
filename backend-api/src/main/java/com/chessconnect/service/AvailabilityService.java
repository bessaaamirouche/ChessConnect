package com.chessconnect.service;

import com.chessconnect.dto.availability.AvailabilityRequest;
import com.chessconnect.dto.availability.AvailabilityResponse;
import com.chessconnect.dto.availability.TimeSlotResponse;
import com.chessconnect.model.Availability;
import com.chessconnect.model.FavoriteTeacher;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.LessonStatus;
import com.chessconnect.repository.AvailabilityRepository;
import com.chessconnect.repository.FavoriteTeacherRepository;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AvailabilityService {

    private static final Logger log = LoggerFactory.getLogger(AvailabilityService.class);
    private static final int SLOT_DURATION_MINUTES = 60;
    private static final int SLOT_INTERVAL_MINUTES = 15; // Créneaux toutes les 15 min pour plus de flexibilité
    private static final int PREMIUM_PRIORITY_HOURS = 24; // Premium users see slots 24h before others

    private final AvailabilityRepository availabilityRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final FavoriteTeacherRepository favoriteRepository;
    private final EmailService emailService;
    private final SubscriptionService subscriptionService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public AvailabilityService(
            AvailabilityRepository availabilityRepository,
            LessonRepository lessonRepository,
            UserRepository userRepository,
            FavoriteTeacherRepository favoriteRepository,
            EmailService emailService,
            SubscriptionService subscriptionService
    ) {
        this.availabilityRepository = availabilityRepository;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.favoriteRepository = favoriteRepository;
        this.emailService = emailService;
        this.subscriptionService = subscriptionService;
    }

    @Transactional
    public AvailabilityResponse createAvailability(Long teacherId, AvailabilityRequest request) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        validateAvailabilityRequest(request);

        // Check for overlapping availabilities (Feature 8)
        checkAvailabilityOverlap(teacherId, request);

        Availability availability = new Availability();
        availability.setTeacher(teacher);
        availability.setDayOfWeek(request.getDayOfWeek());
        availability.setStartTime(request.getStartTime());
        availability.setEndTime(request.getEndTime());
        availability.setIsRecurring(request.getIsRecurring());
        availability.setSpecificDate(request.getSpecificDate());
        availability.setIsActive(true);

        availability = availabilityRepository.save(availability);
        log.info("Created availability {} for teacher {}", availability.getId(), teacherId);

        // Notify subscribed students
        notifySubscribers(teacher, availability);

        return AvailabilityResponse.fromEntity(availability);
    }

    private void notifySubscribers(User teacher, Availability availability) {
        List<FavoriteTeacher> subscribers = favoriteRepository.findByTeacherIdAndNotifyNewSlotsTrue(teacher.getId());

        if (subscribers.isEmpty()) {
            return;
        }

        String teacherName = teacher.getFirstName() + " " + teacher.getLastName();
        String availabilityInfo = formatAvailabilityInfo(availability);
        String bookingLink = frontendUrl + "/book/" + teacher.getId();

        int notifiedCount = 0;
        for (FavoriteTeacher subscriber : subscribers) {
            User student = subscriber.getStudent();

            // Only notify Premium subscribers
            if (!subscriptionService.isPremium(student.getId())) {
                continue;
            }

            emailService.sendNewAvailabilityNotification(
                    student.getEmail(),
                    student.getFirstName(),
                    teacherName,
                    availabilityInfo,
                    bookingLink
            );
            notifiedCount++;
        }

        log.info("Notified {} Premium subscribers about new availability for teacher {}",
                notifiedCount, teacher.getId());
    }

    private String formatAvailabilityInfo(Availability availability) {
        StringBuilder sb = new StringBuilder();

        if (availability.getIsRecurring()) {
            String dayName = availability.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH);
            sb.append("Tous les ").append(dayName).append("s");
        } else {
            sb.append("Le ").append(availability.getSpecificDate().toString());
        }

        sb.append(" de ").append(availability.getStartTime())
          .append(" a ").append(availability.getEndTime());

        return sb.toString();
    }

    public List<AvailabilityResponse> getTeacherAvailabilities(Long teacherId) {
        return availabilityRepository.findByTeacherIdAndIsActiveTrue(teacherId)
                .stream()
                .map(AvailabilityResponse::fromEntity)
                .toList();
    }

    public List<AvailabilityResponse> getRecurringAvailabilities(Long teacherId) {
        return availabilityRepository.findByTeacherIdAndIsRecurringTrueAndIsActiveTrue(teacherId)
                .stream()
                .map(AvailabilityResponse::fromEntity)
                .toList();
    }

    @Transactional
    public void deleteAvailability(Long teacherId, Long availabilityId) {
        Availability availability = availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new RuntimeException("Availability not found"));

        if (!availability.getTeacher().getId().equals(teacherId)) {
            throw new RuntimeException("Unauthorized to delete this availability");
        }

        availability.setIsActive(false);
        availabilityRepository.save(availability);
        log.info("Deactivated availability {} for teacher {}", availabilityId, teacherId);
    }

    public List<TimeSlotResponse> getAvailableSlots(Long teacherId, LocalDate startDate, LocalDate endDate) {
        return getAvailableSlots(teacherId, startDate, endDate, false);
    }

    /**
     * Get available slots for a teacher.
     * Premium users see all slots immediately. Non-premium users only see slots
     * from availabilities created more than 24 hours ago.
     */
    public List<TimeSlotResponse> getAvailableSlots(Long teacherId, LocalDate startDate, LocalDate endDate, boolean isPremiumUser) {
        List<TimeSlotResponse> slots = new ArrayList<>();

        // Get all availabilities for the teacher
        List<Availability> allAvailabilities = availabilityRepository.findByTeacherIdAndIsActiveTrue(teacherId);
        log.info("Found {} active availabilities for teacher {}", allAvailabilities.size(), teacherId);

        // For non-premium users, filter out availabilities created less than 24h ago
        LocalDateTime priorityCutoff = LocalDateTime.now().minusHours(PREMIUM_PRIORITY_HOURS);
        List<Availability> availabilities = isPremiumUser
                ? allAvailabilities
                : allAvailabilities.stream()
                    .filter(a -> a.getCreatedAt() == null || a.getCreatedAt().isBefore(priorityCutoff))
                    .toList();

        log.info("After premium filter (isPremium={}): {} availabilities", isPremiumUser, availabilities.size());

        // Get existing lessons to check for conflicts
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
        List<Lesson> existingLessons = lessonRepository.findByTeacherIdAndScheduledAtBetween(
                teacherId, startDateTime, endDateTime
        );

        List<LocalDateTime> bookedSlotStarts = existingLessons.stream()
                .filter(l -> l.getStatus() != LessonStatus.CANCELLED)
                .map(Lesson::getScheduledAt)
                .toList();

        // Generate slots for each day
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            LocalDate finalDate = currentDate;

            // Find availabilities for this day
            List<Availability> dayAvailabilities = availabilities.stream()
                    .filter(a -> {
                        if (a.getIsRecurring()) {
                            return a.getDayOfWeek() == dayOfWeek;
                        } else {
                            return finalDate.equals(a.getSpecificDate());
                        }
                    })
                    .toList();

            // Generate time slots for each availability
            for (Availability availability : dayAvailabilities) {
                LocalTime currentTime = availability.getStartTime();
                LocalTime endTime = availability.getEndTime();
                // Handle midnight wrap-around: treat 00:00 as end of day
                boolean isMidnightEnd = endTime.equals(LocalTime.MIDNIGHT);

                while (isSlotWithinAvailability(currentTime, endTime, isMidnightEnd)) {
                    LocalDateTime slotDateTime = LocalDateTime.of(finalDate, currentTime);
                    LocalDateTime slotEndDateTime = slotDateTime.plusMinutes(SLOT_DURATION_MINUTES);

                    // Allow urgent bookings - show slots up to 5 min in the past
                    if (slotDateTime.isAfter(LocalDateTime.now().minusMinutes(5))) {
                        // Check for overlaps with booked lessons
                        boolean isAvailable = bookedSlotStarts.stream()
                                .noneMatch(bookedStart -> {
                                    LocalDateTime bookedEnd = bookedStart.plusMinutes(SLOT_DURATION_MINUTES);
                                    return slotDateTime.isBefore(bookedEnd) && slotEndDateTime.isAfter(bookedStart);
                                });

                        slots.add(TimeSlotResponse.create(
                                finalDate,
                                currentTime,
                                currentTime.plusMinutes(SLOT_DURATION_MINUTES),
                                isAvailable
                        ));
                    }

                    currentTime = currentTime.plusMinutes(SLOT_INTERVAL_MINUTES);
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        return slots.stream()
                .sorted((a, b) -> a.getDateTime().compareTo(b.getDateTime()))
                .toList();
    }

    public boolean isSlotAvailable(Long teacherId, LocalDateTime dateTime) {
        LocalDate date = dateTime.toLocalDate();
        LocalTime time = dateTime.toLocalTime();
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        // Check if there's an availability for this slot (no time restriction)
        List<Availability> availabilities = availabilityRepository.findAvailabilitiesForDate(
                teacherId, dayOfWeek, date
        );

        boolean hasAvailability = availabilities.stream()
                .anyMatch(a -> !time.isBefore(a.getStartTime()) &&
                               time.plusMinutes(SLOT_DURATION_MINUTES).compareTo(a.getEndTime()) <= 0);

        if (!hasAvailability) {
            return false;
        }

        // Check if slot is not already booked
        LocalDateTime slotEnd = dateTime.plusMinutes(SLOT_DURATION_MINUTES);
        List<Lesson> conflictingLessons = lessonRepository.findByTeacherIdAndScheduledAtBetween(
                teacherId,
                dateTime.minusMinutes(SLOT_DURATION_MINUTES - 1),
                slotEnd
        );

        return conflictingLessons.stream()
                .noneMatch(l -> l.getStatus() != LessonStatus.CANCELLED);
    }

    private void validateAvailabilityRequest(AvailabilityRequest request) {
        // Handle midnight wrap-around: 23:00 -> 00:00 is valid (treat 00:00 as 24:00)
        LocalTime startTime = request.getStartTime();
        LocalTime endTime = request.getEndTime();
        boolean isMidnightWrapAround = endTime.equals(LocalTime.MIDNIGHT) && startTime.isAfter(LocalTime.of(22, 0));

        if (!isMidnightWrapAround && startTime.isAfter(endTime)) {
            throw new RuntimeException("Start time must be before end time");
        }

        if (request.getIsRecurring() && request.getDayOfWeek() == null) {
            throw new RuntimeException("Day of week is required for recurring availabilities");
        }

        if (!request.getIsRecurring() && request.getSpecificDate() == null) {
            throw new RuntimeException("Specific date is required for non-recurring availabilities");
        }

        if (!request.getIsRecurring() && request.getSpecificDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Specific date cannot be in the past");
        }
    }

    /**
     * Check for overlapping availability slots (Feature 8).
     * Teacher availability slots must not overlap on the same day.
     */
    private void checkAvailabilityOverlap(Long teacherId, AvailabilityRequest request) {
        List<Availability> existingAvailabilities;

        if (request.getIsRecurring()) {
            // For recurring, check overlaps on the same day of week
            existingAvailabilities = availabilityRepository
                    .findByTeacherIdAndDayOfWeekAndIsActiveTrue(teacherId, request.getDayOfWeek())
                    .stream()
                    .filter(Availability::getIsRecurring)
                    .toList();
        } else {
            // For specific date, check overlaps on that date
            existingAvailabilities = availabilityRepository
                    .findAvailabilitiesForDate(teacherId, request.getSpecificDate().getDayOfWeek(), request.getSpecificDate());
        }

        LocalTime newStart = request.getStartTime();
        LocalTime newEnd = request.getEndTime();

        boolean hasOverlap = existingAvailabilities.stream()
                .anyMatch(existing -> {
                    LocalTime existingStart = existing.getStartTime();
                    LocalTime existingEnd = existing.getEndTime();
                    // Check if time ranges overlap
                    return newStart.isBefore(existingEnd) && newEnd.isAfter(existingStart);
                });

        if (hasOverlap) {
            throw new RuntimeException(
                    "Vous avez deja une disponibilite sur ce creneau. Modifiez les horaires ou supprimez l'ancienne."
            );
        }
    }

    /**
     * Check if a slot (starting at currentTime, lasting SLOT_DURATION_MINUTES)
     * fits within the availability window ending at endTime.
     * Handles midnight wrap-around (23:00 -> 00:00).
     */
    private boolean isSlotWithinAvailability(LocalTime currentTime, LocalTime endTime, boolean isMidnightEnd) {
        LocalTime slotEnd = currentTime.plusMinutes(SLOT_DURATION_MINUTES);

        if (isMidnightEnd) {
            // For midnight end (00:00), the slot must start before midnight
            // and the calculated slot end will wrap to 00:00
            return currentTime.getHour() >= 22 && slotEnd.equals(LocalTime.MIDNIGHT);
        }

        // Normal case: slot end must be <= availability end
        return slotEnd.compareTo(endTime) <= 0;
    }
}
