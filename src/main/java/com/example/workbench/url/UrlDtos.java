package com.example.workbench.url;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class UrlDtos {
    private UrlDtos() {}
    public record CreateRequest(@NotBlank @Size(max=2048) String url, Instant expiresAt) {}
    public record UrlResponse(Long id, String code, String shortUrl, String originalUrl, Instant createdAt, Instant expiresAt, long clickCount, Instant lastClickedAt, boolean active, boolean expired) {
        static UrlResponse from(ShortUrl u, String baseUrl) {
            boolean expired = u.expiresAt != null && !u.expiresAt.isAfter(Instant.now());
            return new UrlResponse(u.id, u.code, baseUrl + "/" + u.code, u.originalUrl, u.createdAt, u.expiresAt, u.clickCount, u.lastClickedAt, u.active, expired);
        }
    }
    public record AnalyticsResponse(String code, String shortUrl, String originalUrl, Instant createdAt, Instant expiresAt, long clickCount, Instant lastClickedAt, boolean active, boolean expired) {}
}
