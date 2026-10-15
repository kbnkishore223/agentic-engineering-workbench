package com.example.workbench.url;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;

@Service
public class UrlService {
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final ShortUrlRepository repository;
    private final SecureRandom random = new SecureRandom();
    public UrlService(ShortUrlRepository repository) { this.repository = repository; }

    @Transactional
    public UrlDtos.UrlResponse create(UrlDtos.CreateRequest request, String baseUrl) {
        String raw = request.url().trim();
        URI uri;
        try { uri = URI.create(raw); } catch (Exception e) { throw new IllegalArgumentException("URL is not valid."); }
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null)
            throw new IllegalArgumentException("Only valid http:// and https:// URLs are supported.");
        if (request.expiresAt() != null && !request.expiresAt().isAfter(Instant.now())) throw new IllegalArgumentException("expiresAt must be in the future.");
        ShortUrl entity = new ShortUrl(); entity.originalUrl = uri.toString(); entity.createdAt = Instant.now(); entity.expiresAt = request.expiresAt(); entity.active = true;
        String code; do { code = randomCode(7); } while (repository.existsByCode(code));
        entity.code = code;
        return UrlDtos.UrlResponse.from(repository.save(entity), baseUrl);
    }
    private String randomCode(int length) { StringBuilder b = new StringBuilder(length); for (int i=0;i<length;i++) b.append(ALPHABET.charAt(random.nextInt(ALPHABET.length()))); return b.toString(); }

    @Transactional(readOnly = true)
    public List<UrlDtos.UrlResponse> all(String baseUrl) { return repository.findAll().stream().sorted((a,b) -> b.createdAt.compareTo(a.createdAt)).map(u -> UrlDtos.UrlResponse.from(u, baseUrl)).toList(); }

    @Transactional
    public String resolve(String code) {
        ShortUrl u = repository.findByCode(code).orElseThrow(() -> new IllegalArgumentException("Short URL not found."));
        if (!u.active) throw new IllegalStateException("This short URL has been disabled.");
        if (u.expiresAt != null && !u.expiresAt.isAfter(Instant.now())) throw new IllegalStateException("This short URL has expired.");
        u.clickCount++; u.lastClickedAt = Instant.now(); repository.save(u); return u.originalUrl;
    }

    @Transactional(readOnly = true)
    public UrlDtos.UrlResponse analytics(String code, String baseUrl) {
        ShortUrl u = repository.findByCode(code).orElseThrow(() -> new IllegalArgumentException("Short URL not found."));
        return UrlDtos.UrlResponse.from(u, baseUrl);
    }

    @Transactional(readOnly = true)
    public UrlMetrics metrics() {
        List<ShortUrl> urls = repository.findAll(); long clicks = urls.stream().mapToLong(u -> u.clickCount).sum();
        long expired = urls.stream().filter(u -> u.expiresAt != null && !u.expiresAt.isAfter(Instant.now())).count();
        long active = urls.stream().filter(u -> u.active && (u.expiresAt == null || u.expiresAt.isAfter(Instant.now()))).count();
        return new UrlMetrics(urls.size(), clicks, expired, active);
    }
    public record UrlMetrics(long totalUrls, long totalClicks, long expiredUrls, long activeUrls) {}
}
