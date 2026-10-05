package com.example.shortener.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public final class Dtos {
    private Dtos() {}
    public record CreateRequest(String url, Integer ttlDays) {}
    public record UrlResponse(String shortCode, String shortUrl, String longUrl, Instant expiresAt) {}
    public record UrlSummary(String shortCode, String longUrl, Instant createdAt, long clicks) {}
    public record StatsResponse(String shortCode, String longUrl, long totalClicks, Map<LocalDate, Long> clicksByDay) {}
}
