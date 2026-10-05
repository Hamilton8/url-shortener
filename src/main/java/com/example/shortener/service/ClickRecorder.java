package com.example.shortener.service;

import com.example.shortener.model.ClickEvent;
import com.example.shortener.repo.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** Saves clicks off the request thread so redirects stay fast. */
@Component
public class ClickRecorder {
    private final ClickEventRepository clicks;
    private final UrlMappingRepository urls;
    private final Clock clock;

    public ClickRecorder(ClickEventRepository clicks, UrlMappingRepository urls, Clock clock) {
        this.clicks = clicks; this.urls = urls; this.clock = clock;
    }

    @Async
    public void record(Long urlId, String userAgent, String referrer, String ip) {
        clicks.save(new ClickEvent(urls.getReferenceById(urlId), clock.instant(),
                cut(userAgent, 512), cut(referrer, 2048), sha256(ip)));
    }

    static String cut(String s, int max) { return s == null ? null : s.length() <= max ? s : s.substring(0, max); }

    static String sha256(String s) {
        if (s == null) return null;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
