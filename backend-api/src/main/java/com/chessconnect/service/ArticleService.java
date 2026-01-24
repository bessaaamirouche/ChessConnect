package com.chessconnect.service;

import com.chessconnect.dto.ArticleDetailDTO;
import com.chessconnect.dto.ArticleListDTO;
import com.chessconnect.model.Article;
import com.chessconnect.repository.ArticleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;

    public ArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    public Page<ArticleListDTO> getPublishedArticles(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return articleRepository.findByPublishedTrueOrderByPublishedAtDesc(pageable)
                .map(ArticleListDTO::new);
    }

    public Page<ArticleListDTO> getAllArticles(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return articleRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(ArticleListDTO::new);
    }

    public Page<ArticleListDTO> getArticlesByCategory(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return articleRepository.findByCategoryAndPublishedTrueOrderByPublishedAtDesc(category, pageable)
                .map(ArticleListDTO::new);
    }

    public List<ArticleListDTO> getLatestArticles() {
        return articleRepository.findTop5ByPublishedTrueOrderByPublishedAtDesc()
                .stream()
                .map(ArticleListDTO::new)
                .collect(Collectors.toList());
    }

    public Optional<ArticleDetailDTO> getArticleBySlug(String slug) {
        return articleRepository.findBySlugAndPublishedTrue(slug)
                .map(article -> {
                    ArticleDetailDTO dto = new ArticleDetailDTO(article);
                    // Get related articles (same category, excluding current)
                    List<ArticleListDTO> related = articleRepository
                            .findByCategoryAndPublishedTrueAndIdNotOrderByPublishedAtDesc(
                                    article.getCategory(),
                                    article.getId(),
                                    PageRequest.of(0, 3))
                            .stream()
                            .map(ArticleListDTO::new)
                            .collect(Collectors.toList());
                    dto.setRelatedArticles(related);
                    return dto;
                });
    }

    public Optional<ArticleDetailDTO> getArticleById(Long id) {
        return articleRepository.findById(id)
                .map(ArticleDetailDTO::new);
    }

    public List<String> getAllCategories() {
        return articleRepository.findAllCategories();
    }

    @Transactional
    public Article createArticle(Article article) {
        // Generate slug from title if not provided
        if (article.getSlug() == null || article.getSlug().isEmpty()) {
            article.setSlug(generateSlug(article.getTitle()));
        }

        // Ensure slug is unique
        String baseSlug = article.getSlug();
        String finalSlug = baseSlug;
        int counter = 1;
        while (articleRepository.existsBySlug(finalSlug)) {
            finalSlug = baseSlug + "-" + counter;
            counter++;
        }
        article.setSlug(finalSlug);

        // Calculate reading time
        article.setReadingTimeMinutes(calculateReadingTime(article.getContent()));

        // Set published date if publishing
        if (article.isPublished() && article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }

        return articleRepository.save(article);
    }

    @Transactional
    public Optional<Article> updateArticle(Long id, Article updatedArticle) {
        return articleRepository.findById(id)
                .map(article -> {
                    article.setTitle(updatedArticle.getTitle());
                    article.setMetaDescription(updatedArticle.getMetaDescription());
                    article.setMetaKeywords(updatedArticle.getMetaKeywords());
                    article.setExcerpt(updatedArticle.getExcerpt());
                    article.setContent(updatedArticle.getContent());
                    article.setCoverImage(updatedArticle.getCoverImage());
                    article.setAuthor(updatedArticle.getAuthor());
                    article.setCategory(updatedArticle.getCategory());

                    // Recalculate reading time
                    article.setReadingTimeMinutes(calculateReadingTime(article.getContent()));

                    // Handle publishing
                    if (updatedArticle.isPublished() && !article.isPublished()) {
                        article.setPublishedAt(LocalDateTime.now());
                    }
                    article.setPublished(updatedArticle.isPublished());

                    return articleRepository.save(article);
                });
    }

    @Transactional
    public boolean deleteArticle(Long id) {
        if (articleRepository.existsById(id)) {
            articleRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional
    public Optional<Article> publishArticle(Long id) {
        return articleRepository.findById(id)
                .map(article -> {
                    article.setPublished(true);
                    if (article.getPublishedAt() == null) {
                        article.setPublishedAt(LocalDateTime.now());
                    }
                    return articleRepository.save(article);
                });
    }

    @Transactional
    public Optional<Article> unpublishArticle(Long id) {
        return articleRepository.findById(id)
                .map(article -> {
                    article.setPublished(false);
                    return articleRepository.save(article);
                });
    }

    private String generateSlug(String title) {
        if (title == null) return "";

        // Normalize and remove accents
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String withoutAccents = pattern.matcher(normalized).replaceAll("");

        // Convert to lowercase and replace spaces/special chars with hyphens
        return withoutAccents
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private int calculateReadingTime(String content) {
        if (content == null || content.isEmpty()) {
            return 1;
        }
        // Average reading speed: 200 words per minute
        int wordCount = content.split("\\s+").length;
        int minutes = (int) Math.ceil(wordCount / 200.0);
        return Math.max(1, minutes);
    }
}
