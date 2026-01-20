package com.chessconnect.dto;

import com.chessconnect.model.Article;
import java.time.LocalDateTime;

public class ArticleListDTO {
    private Long id;
    private String title;
    private String slug;
    private String excerpt;
    private String coverImage;
    private String author;
    private LocalDateTime publishedAt;
    private String category;
    private Integer readingTimeMinutes;

    public ArticleListDTO() {}

    public ArticleListDTO(Article article) {
        this.id = article.getId();
        this.title = article.getTitle();
        this.slug = article.getSlug();
        this.excerpt = article.getExcerpt();
        this.coverImage = article.getCoverImage();
        this.author = article.getAuthor();
        this.publishedAt = article.getPublishedAt();
        this.category = article.getCategory();
        this.readingTimeMinutes = article.getReadingTimeMinutes();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getReadingTimeMinutes() {
        return readingTimeMinutes;
    }

    public void setReadingTimeMinutes(Integer readingTimeMinutes) {
        this.readingTimeMinutes = readingTimeMinutes;
    }
}
