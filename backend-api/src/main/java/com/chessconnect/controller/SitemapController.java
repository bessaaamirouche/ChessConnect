package com.chessconnect.controller;

import com.chessconnect.model.Article;
import com.chessconnect.repository.ArticleRepository;
import com.chessconnect.repository.UserRepository;
import com.chessconnect.model.User;
import com.chessconnect.model.enums.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
public class SitemapController {

    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public SitemapController(ArticleRepository articleRepository, UserRepository userRepository) {
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getSitemap() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // Static pages
        addUrl(xml, "", "1.0", "daily");
        addUrl(xml, "/coaches", "0.9", "daily");
        addUrl(xml, "/blog", "0.9", "daily");
        addUrl(xml, "/about", "0.7", "monthly");
        addUrl(xml, "/how-it-works", "0.7", "monthly");
        addUrl(xml, "/pricing", "0.7", "monthly");
        addUrl(xml, "/faq", "0.7", "monthly");
        addUrl(xml, "/register", "0.7", "monthly");
        addUrl(xml, "/login", "0.5", "monthly");

        // Teacher profile pages (using UUID for privacy)
        List<User> teachers = userRepository.findByRole(UserRole.TEACHER);
        for (User teacher : teachers) {
            addUrl(xml, "/coaches/" + teacher.getUuid(), "0.8", "weekly");
        }

        // Blog articles
        List<Article> articles = articleRepository.findByPublishedTrue();
        for (Article article : articles) {
            String lastMod = article.getPublishedAt() != null
                ? article.getPublishedAt().format(DateTimeFormatter.ISO_DATE)
                : LocalDateTime.now().format(DateTimeFormatter.ISO_DATE);
            addUrlWithDate(xml, "/blog/" + article.getSlug(), "0.8", "monthly", lastMod);
        }

        xml.append("</urlset>");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(xml.toString());
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getRobotsTxt() {
        String sitemapUrl = frontendUrl.replace(":4200", ":8282");
        String robots = """
            User-agent: *
            Allow: /
            Disallow: /dashboard
            Disallow: /lessons
            Disallow: /book/
            Disallow: /progress
            Disallow: /quiz
            Disallow: /subscription
            Disallow: /availability
            Disallow: /settings
            Disallow: /admin
            Disallow: /wallet
            Disallow: /invoices
            Disallow: /library
            Disallow: /exercise

            # AI Crawlers - Explicitly allowed
            User-agent: GPTBot
            Allow: /

            User-agent: ChatGPT-User
            Allow: /

            User-agent: Google-Extended
            Allow: /

            User-agent: ClaudeBot
            Allow: /

            User-agent: anthropic-ai
            Allow: /

            User-agent: PerplexityBot
            Allow: /

            User-agent: Bingbot
            Allow: /

            User-agent: Applebot
            Allow: /

            User-agent: Meta-ExternalAgent
            Allow: /

            # LLM-optimized content
            # See %s/llms.txt (compact)
            # See %s/llms-full.txt (detailed)

            Sitemap: %s/api/sitemap.xml
            """.formatted(frontendUrl, frontendUrl, sitemapUrl);

        return ResponseEntity.ok(robots);
    }

    private void addUrl(StringBuilder xml, String path, String priority, String changefreq) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(frontendUrl).append(path).append("</loc>\n");
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }

    private void addUrlWithDate(StringBuilder xml, String path, String priority, String changefreq, String lastmod) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(frontendUrl).append(path).append("</loc>\n");
        xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }
}
