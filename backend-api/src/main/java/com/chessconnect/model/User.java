package com.chessconnect.model;

import com.chessconnect.model.enums.UserRole;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public Integer getEloRating() { return eloRating; }
    public void setEloRating(Integer eloRating) { this.eloRating = eloRating; }

    public Integer getAge() {
        if (birthDate == null) return null;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
