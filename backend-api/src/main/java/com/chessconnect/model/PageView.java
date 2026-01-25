package com.chessconnect.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "page_views", indexes = {
    @Index(name = "idx_page_view_timestamp", columnList = "visited_at"),
    @Index(name = "idx_page_view_session", columnList = "session_id"),
    @Index(name = "idx_page_view_user", columnList = "user_id")
})
public class PageView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column(name = "page_url", nullable = false, length = 500)
    private String pageUrl;

    @Column(name = "session_id", length = 36)
    private String sessionId;

    @Column(name = "visited_at", nullable = false)
    private LocalDateTime visitedAt;

    @PrePersist
    protected void onCreate() {
        if (visitedAt == null) {
            visitedAt = LocalDateTime.now();
        }
    }

    // Constructors
    public PageView() {}

    public PageView(User user, String pageUrl, String sessionId) {
        this.user = user;
        this.pageUrl = pageUrl;
        this.sessionId = sessionId;
        this.visitedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public LocalDateTime getVisitedAt() {
        return visitedAt;
    }

    public void setVisitedAt(LocalDateTime visitedAt) {
        this.visitedAt = visitedAt;
    }
}
