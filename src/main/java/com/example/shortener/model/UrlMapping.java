package com.example.shortener.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "url_mapping")
public class UrlMapping {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "short_code", nullable = false, unique = true, length = 16)
    private String shortCode;
    @Column(name = "long_url", nullable = false, length = 2048)
    private String longUrl;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "expires_at")
    private Instant expiresAt;

    protected UrlMapping() {}
    public UrlMapping(String shortCode, String longUrl, Instant createdAt, Instant expiresAt) {
        this.shortCode = shortCode; this.longUrl = longUrl; this.createdAt = createdAt; this.expiresAt = expiresAt;
    }
    public Long getId() { return id; }
    public String getShortCode() { return shortCode; }
    public String getLongUrl() { return longUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
