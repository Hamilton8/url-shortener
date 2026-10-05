package com.example.shortener.service;

import com.example.shortener.dto.Dtos.*;
import com.example.shortener.model.UrlMapping;
import com.example.shortener.repo.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UrlService {
    static final int MAX_URL_LENGTH = 2048, CODE_LENGTH = 7, MAX_ATTEMPTS = 5;

    private final UrlMappingRepository urls;
    private final ClickEventRepository clicks;
    private final Clock clock;
    private final Random random = new SecureRandom();

    public UrlService(UrlMappingRepository urls, ClickEventRepository clicks, Clock clock) {
        this.urls = urls; this.clicks = clicks; this.clock = clock;
    }

    public UrlMapping create(String rawUrl, Integer ttlDays) {
        String url = validate(rawUrl);
        if (ttlDays != null && ttlDays <= 0) throw bad("ttlDays must be positive");
        Instant now = clock.instant();
        Instant expires = ttlDays == null ? null : now.plus(Duration.ofDays(ttlDays));
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String code = CodeGenerator.generate(random, CODE_LENGTH);
            if (!urls.existsByShortCode(code)) return urls.save(new UrlMapping(code, url, now, expires));
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not allocate a short code");
    }

    public UrlMapping resolve(String code) {
        UrlMapping m = find(code);
        if (m.getExpiresAt() != null && !m.getExpiresAt().isAfter(clock.instant()))
            throw new ResponseStatusException(HttpStatus.GONE, "Link expired");
        return m;
    }

    public StatsResponse stats(String code) {
        UrlMapping m = find(code);
        List<Instant> times = clicks.findClickTimes(m.getId());
        Map<LocalDate, Long> byDay = times.stream().collect(Collectors.groupingBy(
                t -> t.atZone(ZoneOffset.UTC).toLocalDate(), TreeMap::new, Collectors.counting()));
        return new StatsResponse(m.getShortCode(), m.getLongUrl(), times.size(), byDay);
    }

    public List<UrlSummary> list() {
        return urls.findAllByOrderByCreatedAtDesc().stream()
                .map(m -> new UrlSummary(m.getShortCode(), m.getLongUrl(), m.getCreatedAt(), clicks.countByUrlMappingId(m.getId())))
                .toList();
    }

    private UrlMapping find(String code) {
        return urls.findByShortCode(code).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown short code"));
    }

    static String validate(String raw) {
        if (raw == null || raw.isBlank()) throw bad("URL is required");
        String url = raw.trim();
        if (url.length() > MAX_URL_LENGTH) throw bad("URL too long");
        try {
            URI u = new URI(url);
            String s = u.getScheme();
            if (s == null || !(s.equalsIgnoreCase("http") || s.equalsIgnoreCase("https")) || u.getHost() == null)
                throw bad("Only absolute http/https URLs are allowed");
        } catch (URISyntaxException e) { throw bad("Malformed URL"); }
        return url;
    }

    private static ResponseStatusException bad(String msg) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg); }
}
