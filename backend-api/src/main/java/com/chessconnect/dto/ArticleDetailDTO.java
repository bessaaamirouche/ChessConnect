package com.chessconnect.dto;

import com.chessconnect.model.Article;
import java.time.LocalDateTime;
import java.util.List;

public class ArticleDetailDTO {
    private Long id;
    private String title;
    private String slug;
    private String metaDescription;
    private String metaKeywords;
    private String excerpt;
    private String content;
    private String coverImage;
    private String author;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
    private boolean published;
    private String category;
    private Integer readingTimeMinutes;
    private List<ArticleListDTO> relatedArticles;

    public ArticleDetailDTO() {}

    public ArticleDetailDTO(Article article) {
        this.id = article.getId();
        this.title = article.getTitle();
        this.slug = article.getSlug();
        this.metaDescription = article.getMetaDescription();
        this.metaKeywords = article.getMetaKeywords();
        this.excerpt = article.getExcerpt();
        this.content = article.getContent();
        this.coverImage = article.getCoverImage();
        this.author = article.getAuthor();
        this.createdAt = article.getCreatedAt();
        this.publishedAt = article.getPublishedAt();
        this.updatedAt = article.getUpdatedAt();
        this.published = article.isPublished();
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

    public String getMetaDescription() {
        return metaDescription;
    }

    public void setMetaDescription(String metaDescription) {
        this.metaDescription = metaDescription;
    }

    public String getMetaKeywords() {
        return metaKeywords;
    }

    public void setMetaKeywords(String metaKeywords) {
        this.metaKeywords = metaKeywords;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
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

    public List<ArticleListDTO> getRelatedArticles() {
        return relatedArticles;
    }

    public void setRelatedArticles(List<ArticleListDTO> relatedArticles) {
        this.relatedArticles = relatedArticles;
    }
}
