package com.chessconnect.repository;

import com.chessconnect.model.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    Optional<Article> findBySlug(String slug);

    Optional<Article> findBySlugAndPublishedTrue(String slug);

    Page<Article> findByPublishedTrueOrderByPublishedAtDesc(Pageable pageable);

    Page<Article> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Article> findByCategoryAndPublishedTrueOrderByPublishedAtDesc(String category, Pageable pageable);

    List<Article> findTop5ByPublishedTrueOrderByPublishedAtDesc();

    List<Article> findByPublishedTrue();

    List<Article> findByCategoryAndPublishedTrueAndIdNotOrderByPublishedAtDesc(String category, Long id, Pageable pageable);

    @Query("SELECT DISTINCT a.category FROM Article a WHERE a.published = true")
    List<String> findAllCategories();

    boolean existsBySlug(String slug);

    long countByPublishedTrue();
}
