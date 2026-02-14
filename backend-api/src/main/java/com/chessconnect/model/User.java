package com.chessconnect.model;

import com.chessconnect.model.enums.UserRole;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_role", columnList = "role"),
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_last_login", columnList = "last_login_at"),
    @Index(name = "idx_user_uuid", columnList = "uuid")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, unique = true, updatable = false)
    private String uuid;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "hourly_rate_cents")
    private Integer hourlyRateCents;

    @Column(name = "accepts_subscription")
    private Boolean acceptsSubscription = true;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "elo_rating")
    private Integer eloRating;

    // Languages spoken by teacher (comma-separated: "FR,EN,ES")
    @Column(name = "languages")
    private String languages;

    // Email reminder preference
    @Column(name = "email_reminders_enabled")
    private Boolean emailRemindersEnabled = true;

    // Push notification preference
    @Column(name = "push_notifications_enabled")
    private Boolean pushNotificationsEnabled = true;

    // Admin suspension
    @Column(name = "is_suspended")
    private Boolean isSuspended = false;

    // Email verification
    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    // Teacher banking information
    @Column(name = "iban")
    private String iban;

    @Column(name = "bic")
    private String bic;

    @Column(name = "account_holder_name")
    private String accountHolderName;

    @Column(name = "siret")
    private String siret;

    @Column(name = "company_name")
    private String companyName;

    // Stripe Connect integration
    @Column(name = "stripe_connect_account_id")
    private String stripeConnectAccountId;

    @Column(name = "stripe_connect_onboarding_complete")
    private Boolean stripeConnectOnboardingComplete = false;

    // Current course tracking for students (programme pedagogique)
    @Column(name = "current_course_id")
    private Integer currentCourseId = 1;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // Online presence tracking (updated every 30 seconds by heartbeat)
    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    // Premium trial without credit card (14 days free)
    @Column(name = "premium_trial_end")
    private LocalDate premiumTrialEnd;

    // Referral tracking
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referred_by_code_id")
    private PromoCode referredByCode;

    @Column(name = "referral_code_used_at")
    private LocalDateTime referralCodeUsedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
    private List<Lesson> lessonsAsStudent = new ArrayList<>();

    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL)
    private List<Lesson> lessonsAsTeacher = new ArrayList<>();

    @OneToOne(mappedBy = "student", cascade = CascadeType.ALL)
    private Progress progress;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
    private List<Subscription> subscriptions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUuid() { return uuid; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public Integer getHourlyRateCents() { return hourlyRateCents; }
    public void setHourlyRateCents(Integer hourlyRateCents) { this.hourlyRateCents = hourlyRateCents; }

    public Boolean getAcceptsSubscription() { return acceptsSubscription; }
    public void setAcceptsSubscription(Boolean acceptsSubscription) { this.acceptsSubscription = acceptsSubscription; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public List<Lesson> getLessonsAsStudent() { return lessonsAsStudent; }
    public List<Lesson> getLessonsAsTeacher() { return lessonsAsTeacher; }

    public Progress getProgress() { return progress; }
    public void setProgress(Progress progress) { this.progress = progress; }

    public List<Subscription> getSubscriptions() { return subscriptions; }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Returns abbreviated name: "FirstName L." (for privacy)
     */
    public String getDisplayName() {
        if (lastName == null || lastName.isEmpty()) {
            return firstName;
        }
        return firstName + " " + lastName.charAt(0) + ".";
    }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public Integer getEloRating() { return eloRating; }
    public void setEloRating(Integer eloRating) { this.eloRating = eloRating; }

    public Integer getAge() {
        if (birthDate == null) return null;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public String getLanguages() { return languages; }
    public void setLanguages(String languages) { this.languages = languages; }

    public Boolean getEmailRemindersEnabled() { return emailRemindersEnabled; }
    public void setEmailRemindersEnabled(Boolean emailRemindersEnabled) { this.emailRemindersEnabled = emailRemindersEnabled; }

    public Boolean getPushNotificationsEnabled() { return pushNotificationsEnabled; }
    public void setPushNotificationsEnabled(Boolean pushNotificationsEnabled) { this.pushNotificationsEnabled = pushNotificationsEnabled; }

    public Boolean getIsSuspended() { return isSuspended; }
    public void setIsSuspended(Boolean isSuspended) { this.isSuspended = isSuspended; }

    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public String getBic() { return bic; }
    public void setBic(String bic) { this.bic = bic; }

    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }

    public String getSiret() { return siret; }
    public void setSiret(String siret) { this.siret = siret; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getStripeConnectAccountId() { return stripeConnectAccountId; }
    public void setStripeConnectAccountId(String stripeConnectAccountId) { this.stripeConnectAccountId = stripeConnectAccountId; }

    public Boolean getStripeConnectOnboardingComplete() { return stripeConnectOnboardingComplete; }
    public void setStripeConnectOnboardingComplete(Boolean stripeConnectOnboardingComplete) { this.stripeConnectOnboardingComplete = stripeConnectOnboardingComplete; }

    public Integer getCurrentCourseId() { return currentCourseId; }
    public void setCurrentCourseId(Integer currentCourseId) { this.currentCourseId = currentCourseId; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public LocalDateTime getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(LocalDateTime lastActiveAt) { this.lastActiveAt = lastActiveAt; }

    public LocalDate getPremiumTrialEnd() { return premiumTrialEnd; }
    public void setPremiumTrialEnd(LocalDate premiumTrialEnd) { this.premiumTrialEnd = premiumTrialEnd; }

    public PromoCode getReferredByCode() { return referredByCode; }
    public void setReferredByCode(PromoCode referredByCode) { this.referredByCode = referredByCode; }

    public LocalDateTime getReferralCodeUsedAt() { return referralCodeUsedAt; }
    public void setReferralCodeUsedAt(LocalDateTime referralCodeUsedAt) { this.referralCodeUsedAt = referralCodeUsedAt; }

    /**
     * Check if user has an active premium trial (not expired)
     */
    public boolean hasActivePremiumTrial() {
        return premiumTrialEnd != null && !LocalDate.now().isAfter(premiumTrialEnd);
    }

    // Check if user was active in the last minute
    public boolean isOnline() {
        if (lastActiveAt == null) return false;
        return lastActiveAt.isAfter(LocalDateTime.now().minusMinutes(1));
    }
}
