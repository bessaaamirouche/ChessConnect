package com.chessconnect.service;

import com.chessconnect.dto.availability.AvailabilityRequest;
import com.chessconnect.dto.availability.AvailabilityResponse;
import com.chessconnect.dto.availability.TimeSlotResponse;
import com.chessconnect.event.NotificationEvent;
import com.chessconnect.event.payload.AvailabilityPayload;
import com.chessconnect.model.Availability;
import com.chessconnect.model.FavoriteTeacher;
import com.chessconnect.model.Lesson;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.LessonStatus;
import com.chessconnect.model.enums.LessonType;
import com.chessconnect.model.GroupInvitation;
import com.chessconnect.model.LessonParticipant;
import com.chessconnect.repository.AvailabilityRepository;
import com.chessconnect.repository.FavoriteTeacherRepository;
import com.chessconnect.repository.GroupInvitationRepository;
import com.chessconnect.repository.LessonParticipantRepository;
import com.chessconnect.repository.LessonRepository;
import com.chessconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
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
import java.util.Optional;

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
    private final GroupInvitationRepository groupInvitationRepository;
    private final LessonParticipantRepository participantRepository;
    private final EmailService emailService;
    private final WebPushService webPushService;
    private final SubscriptionService subscriptionService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public AvailabilityService(
            AvailabilityRepository availabilityRepository,
            LessonRepository lessonRepository,
            UserRepository userRepository,
            FavoriteTeacherRepository favoriteRepository,
            GroupInvitationRepository groupInvitationRepository,
            LessonParticipantRepository participantRepository,
            EmailService emailService,
            WebPushService webPushService,
            SubscriptionService subscriptionService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.availabilityRepository = availabilityRepository;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.favoriteRepository = favoriteRepository;
        this.groupInvitationRepository = groupInvitationRepository;
        this.participantRepository = participantRepository;
        this.emailService = emailService;
        this.webPushService = webPushService;
        this.subscriptionService = subscriptionService;
        this.eventPublisher = eventPublisher;
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
        availability.setLessonType(LessonType.valueOf(request.getLessonType()));

        // For GROUP availabilities, coach must specify maxParticipants (2 or 3)
        if (LessonType.GROUP == LessonType.valueOf(request.getLessonType())) {
            if (request.getMaxParticipants() == null || request.getMaxParticipants() < 2 || request.getMaxParticipants() > 3) {
                throw new RuntimeException("errors.groupSizeInvalid");
            }
            availability.setMaxParticipants(request.getMaxParticipants());
        }

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
        int sseNotifiedCount = 0;
        for (FavoriteTeacher subscriber : subscribers) {
            User student = subscriber.getStudent();

            // Send email notification to all subscribers (Premium restriction removed for launch)
            emailService.sendNewAvailabilityNotification(
                    student.getEmail(),
                    student.getFirstName(),
                    teacherName,
                    availabilityInfo,
                    bookingLink
            );
            notifiedCount++;

            // Send Web Push notification
            webPushService.sendToUser(
                    student.getId(),
                    "Nouveau créneau - " + teacherName,
                    availabilityInfo,
                    bookingLink
            );

            // Send SSE notification to all subscribed students
            publishAvailabilityEvent(student.getId(), teacher, availability, availabilityInfo);
            sseNotifiedCount++;
        }

        log.info("Notified {} subscribers (email) and {} subscribers (SSE) about new availability for teacher {}",
                notifiedCount, sseNotifiedCount, teacher.getId());
    }

    /**
     * Publish SSE event when a new availability is created.
     */
    private void publishAvailabilityEvent(Long studentId, User teacher, Availability availability, String dayInfo) {
        try {
            String teacherName = teacher.getFirstName() + " " + teacher.getLastName();
            String timeRange = availability.getStartTime() + " - " + availability.getEndTime();

            AvailabilityPayload payload = new AvailabilityPayload(
                    availability.getId(),
                    teacher.getId(),
                    teacherName,
                    dayInfo,
                    timeRange
            );

            eventPublisher.publishEvent(new NotificationEvent(
                    this,
                    NotificationEvent.EventType.AVAILABILITY_CREATED,
                    studentId,
                    payload
            ));
        } catch (Exception e) {
            log.warn("Failed to publish SSE availability event: {}", e.getMessage());
        }
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
        return getAvailableSlots(teacherId, startDate, endDate, false, null);
    }

    public List<TimeSlotResponse> getAvailableSlots(Long teacherId, LocalDate startDate, LocalDate endDate, boolean isPremiumUser) {
        return getAvailableSlots(teacherId, startDate, endDate, isPremiumUser, null);
    }

    /**
     * Get available slots for a teacher.
     * Premium users see all slots immediately. Non-premium users only see slots
     * from availabilities created more than 24 hours ago.
     * Optionally filter by lesson type (INDIVIDUAL or GROUP).
     */
    public List<TimeSlotResponse> getAvailableSlots(Long teacherId, LocalDate startDate, LocalDate endDate, boolean isPremiumUser, String lessonType) {
        List<TimeSlotResponse> slots = new ArrayList<>();

        // Get all availabilities for the teacher
        List<Availability> allAvailabilities = availabilityRepository.findByTeacherIdAndIsActiveTrue(teacherId);
        log.info("Found {} active availabilities for teacher {}", allAvailabilities.size(), teacherId);

        // For non-premium users, filter out INDIVIDUAL availabilities created less than 24h ago
        // Group lessons have no delay — they are visible immediately for everyone
        LocalDateTime priorityCutoff = LocalDateTime.now().minusHours(PREMIUM_PRIORITY_HOURS);
        List<Availability> availabilities = isPremiumUser
                ? allAvailabilities
                : allAvailabilities.stream()
                    .filter(a -> a.getLessonType() == LessonType.GROUP
                            || a.getCreatedAt() == null
                            || a.getCreatedAt().isBefore(priorityCutoff))
                    .toList();

        log.info("After premium filter (isPremium={}): {} availabilities", isPremiumUser, availabilities.size());

        // Filter by lesson type if specified
        if (lessonType != null && !lessonType.isEmpty()) {
            LessonType type = LessonType.valueOf(lessonType);
            availabilities = availabilities.stream()
                    .filter(a -> a.getLessonType() == type)
                    .toList();
            log.info("After lessonType filter ({}): {} availabilities", lessonType, availabilities.size());
        }

        // Get existing lessons to check for conflicts
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
        List<Lesson> existingLessons = lessonRepository.findByTeacherIdAndScheduledAtBetween(
                teacherId, startDateTime, endDateTime
        );

        List<LocalDateTime> bookedSlotStarts = existingLessons.stream()
                .filter(l -> l.getStatus() != LessonStatus.CANCELLED && l.getStatus() != LessonStatus.COMPLETED)
                // Don't block slot if it's an open (not full) group lesson — other students can still join
                .filter(l -> !(Boolean.TRUE.equals(l.getIsGroupLesson()) && "OPEN".equals(l.getGroupStatus())))
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
                LocalTime startTime = availability.getStartTime();
                LocalTime endTime = availability.getEndTime();
                // Handle midnight wrap-around (e.g., 23:00->00:00 or 23:30->00:30)
                boolean isMidnightCrossing = endTime.isBefore(startTime) || endTime.equals(LocalTime.MIDNIGHT);

                while (isSlotWithinAvailability(currentTime, startTime, endTime, isMidnightCrossing)) {
                    // If slot has crossed midnight, use the next day's date
                    LocalDate slotDate = (isMidnightCrossing && currentTime.isBefore(startTime)) ? finalDate.plusDays(1) : finalDate;
                    LocalDateTime slotDateTime = LocalDateTime.of(slotDate, currentTime);
                    LocalDateTime slotEndDateTime = slotDateTime.plusMinutes(SLOT_DURATION_MINUTES);

                    // Allow urgent bookings - show slots up to 5 min in the past
                    if (slotDateTime.isAfter(LocalDateTime.now().minusMinutes(5))) {
                        // Check for overlaps with booked lessons
                        boolean isAvailable = bookedSlotStarts.stream()
                                .noneMatch(bookedStart -> {
                                    LocalDateTime bookedEnd = bookedStart.plusMinutes(SLOT_DURATION_MINUTES);
                                    return slotDateTime.isBefore(bookedEnd) && slotEndDateTime.isAfter(bookedStart);
                                });

                        // Enrich GROUP slots with group lesson info
                        if (availability.getLessonType() == LessonType.GROUP && availability.getMaxParticipants() != null) {
                            int maxP = availability.getMaxParticipants();
                            Optional<Lesson> openGroup = lessonRepository.findOpenGroupLesson(
                                    teacherId, slotDateTime, slotEndDateTime);

                            Long groupLessonId = null;
                            int currentP = 0;
                            String invToken = null;

                            if (openGroup.isPresent()) {
                                Lesson gl = openGroup.get();
                                groupLessonId = gl.getId();
                                currentP = participantRepository.countByLessonIdAndStatus(gl.getId(), "ACTIVE");
                                invToken = groupInvitationRepository.findByLessonId(gl.getId())
                                        .map(GroupInvitation::getToken).orElse(null);
                                // Full group = not available
                                if (currentP >= maxP) {
                                    isAvailable = false;
                                }
                            }

                            int teacherRate = userRepository.findById(teacherId)
                                    .map(User::getHourlyRateCents).orElse(0);
                            int pricePerPerson = GroupPricingCalculator.calculateParticipantPrice(teacherRate, maxP);

                            slots.add(TimeSlotResponse.createGroupSlot(
                                    slotDate,
                                    currentTime,
                                    currentTime.plusMinutes(SLOT_DURATION_MINUTES),
                                    isAvailable,
                                    maxP,
                                    groupLessonId,
                                    currentP,
                                    invToken,
                                    pricePerPerson
                            ));
                        } else {
                            slots.add(TimeSlotResponse.create(
                                    slotDate,
                                    currentTime,
                                    currentTime.plusMinutes(SLOT_DURATION_MINUTES),
                                    isAvailable,
                                    availability.getLessonType().name()
                            ));
                        }
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
                .anyMatch(a -> {
                    boolean midnightCrossing = a.getEndTime().isBefore(a.getStartTime()) || a.getEndTime().equals(LocalTime.MIDNIGHT);
                    return isSlotWithinAvailability(time, a.getStartTime(), a.getEndTime(), midnightCrossing);
                });

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
                .noneMatch(l -> l.getStatus() != LessonStatus.CANCELLED
                        && l.getStatus() != LessonStatus.COMPLETED
                        && !(Boolean.TRUE.equals(l.getIsGroupLesson()) && "OPEN".equals(l.getGroupStatus())));
    }

    private void validateAvailabilityRequest(AvailabilityRequest request) {
        // Handle midnight wrap-around: e.g., 23:30 -> 00:30 is valid (crosses midnight)
        LocalTime startTime = request.getStartTime();
        LocalTime endTime = request.getEndTime();
        boolean isMidnightCrossing = startTime.getHour() >= 22
                && (endTime.isBefore(startTime) || endTime.equals(LocalTime.MIDNIGHT));

        if (!isMidnightCrossing && startTime.isAfter(endTime)) {
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
                .anyMatch(existing -> timesOverlap(
                        newStart, newEnd,
                        existing.getStartTime(), existing.getEndTime()
                ));

        if (hasOverlap) {
            throw new RuntimeException("errors.availabilityOverlap");
        }
    }

    /**
     * Check if two time ranges overlap, handling midnight wrap-around.
     * Uses minutes-since-midnight with +1440 normalization for ranges crossing midnight.
     */
    private boolean timesOverlap(LocalTime s1, LocalTime e1, LocalTime s2, LocalTime e2) {
        int ms1 = s1.getHour() * 60 + s1.getMinute();
        int me1 = e1.getHour() * 60 + e1.getMinute();
        int ms2 = s2.getHour() * 60 + s2.getMinute();
        int me2 = e2.getHour() * 60 + e2.getMinute();

        // Handle midnight crossover: if end <= start, add a full day
        if (me1 <= ms1) me1 += 1440;
        if (me2 <= ms2) me2 += 1440;

        return ms1 < me2 && me1 > ms2;
    }

    /**
     * Check if a slot (starting at currentTime, lasting SLOT_DURATION_MINUTES)
     * fits within the availability window [startTime, endTime].
     * Handles midnight wrap-around (e.g., 23:00 -> 00:30).
     */
    private boolean isSlotWithinAvailability(LocalTime currentTime, LocalTime startTime, LocalTime endTime, boolean isMidnightCrossing) {
        int currentMinutes = currentTime.getHour() * 60 + currentTime.getMinute();
        int startMinutes = startTime.getHour() * 60 + startTime.getMinute();
        int slotEndMinutes = currentMinutes + SLOT_DURATION_MINUTES;
        int availEndMinutes = endTime.getHour() * 60 + endTime.getMinute();

        if (isMidnightCrossing) {
            availEndMinutes += 1440;
            if (currentMinutes < startMinutes) {
                // currentTime has wrapped past midnight
                slotEndMinutes += 1440;
            }
        }

        return slotEndMinutes <= availEndMinutes;
    }
}
