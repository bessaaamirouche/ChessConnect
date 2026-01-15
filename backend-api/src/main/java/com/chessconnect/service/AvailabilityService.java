package com.chessconnect.service;

import com.chessconnect.dto.availability.AvailabilityRequest;
import com.chessconnect.dto.availability.AvailabilityResponse;
import com.chessconnect.dto.availability.TimeSlotResponse;
import com.chessconnect.model.Availability;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.LessonStatus;
import com.chessconnect.repository.AvailabilityRepository;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AvailabilityService {

    private static final Logger log = LoggerFactory.getLogger(AvailabilityService.class);
    private static final int SLOT_DURATION_MINUTES = 60;
    private static final int SLOT_INTERVAL_MINUTES = 15; // Créneaux toutes les 15 min pour plus de flexibilité

    private final AvailabilityRepository availabilityRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;

    public AvailabilityService(
            AvailabilityRepository availabilityRepository,
            LessonRepository lessonRepository,
            UserRepository userRepository
    ) {
        this.availabilityRepository = availabilityRepository;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AvailabilityResponse createAvailability(Long teacherId, AvailabilityRequest request) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        validateAvailabilityRequest(request);

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

        return AvailabilityResponse.fromEntity(availability);
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
        List<TimeSlotResponse> slots = new ArrayList<>();

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

        // Generate slots 24/7 for each day (from 6:00 to 23:00)
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            LocalDate finalDate = currentDate;

            // Generate time slots from 6:00 to 23:00 every hour
            LocalTime currentTime = LocalTime.of(6, 0);
            LocalTime endOfDay = LocalTime.of(23, 0);

            while (currentTime.isBefore(endOfDay)) {
                LocalDateTime slotDateTime = LocalDateTime.of(finalDate, currentTime);
                LocalDateTime slotEndDateTime = slotDateTime.plusMinutes(SLOT_DURATION_MINUTES);

                // Include slots if start time is at least 15 min in the future (allow urgent bookings)
                if (slotDateTime.isAfter(LocalDateTime.now().minusMinutes(15))) {
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

                currentTime = currentTime.plusHours(1); // Créneaux toutes les heures
            }

            currentDate = currentDate.plusDays(1);
        }

        return slots.stream()
                .sorted((a, b) -> a.getDateTime().compareTo(b.getDateTime()))
                .toList();
    }

    public boolean isSlotAvailable(Long teacherId, LocalDateTime dateTime) {
        // Allow any slot between 6:00 and 23:00
        LocalTime time = dateTime.toLocalTime();
        if (time.isBefore(LocalTime.of(6, 0)) || time.isAfter(LocalTime.of(22, 0))) {
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
        if (request.getStartTime().isAfter(request.getEndTime())) {
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
}
