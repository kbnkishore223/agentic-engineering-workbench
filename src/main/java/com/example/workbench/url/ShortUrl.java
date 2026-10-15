package com.example.workbench.url;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "short_urls", indexes = @Index(name = "idx_short_url_code", columnList = "code", unique = true))
public class ShortUrl {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, unique = true, length = 16) public String code;
    @Column(nullable = false, length = 2048) public String originalUrl;
    @Column(nullable = false) public Instant createdAt;
    public Instant expiresAt;
    @Column(nullable = false) public long clickCount = 0;
    public Instant lastClickedAt;
    @Column(nullable = false) public boolean active = true;
    public ShortUrl() {}
}
