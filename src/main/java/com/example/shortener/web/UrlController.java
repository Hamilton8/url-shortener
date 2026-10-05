package com.example.shortener.web;

import com.example.shortener.dto.Dtos.*;
import com.example.shortener.model.UrlMapping;
import com.example.shortener.service.*;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class UrlController {
    private final UrlService service;
    private final ClickRecorder recorder;
    private final String baseUrl;

    public UrlController(UrlService service, ClickRecorder recorder, @Value("${app.base-url}") String baseUrl) {
        this.service = service; this.recorder = recorder; this.baseUrl = baseUrl;
    }

    @PostMapping("/api/urls")
    public ResponseEntity<UrlResponse> create(@RequestBody CreateRequest req) {
        UrlMapping m = service.create(req.url(), req.ttlDays());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UrlResponse(m.getShortCode(), baseUrl + "/" + m.getShortCode(), m.getLongUrl(), m.getExpiresAt()));
    }

    @GetMapping("/api/urls")
    public List<UrlSummary> list() { return service.list(); }

    @GetMapping("/api/urls/{code}/stats")
    public StatsResponse stats(@PathVariable String code) { return service.stats(code); }

    @GetMapping("/{code:[0-9A-Za-z]{4,16}}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest req) {
        UrlMapping m = service.resolve(code);
        recorder.record(m.getId(), req.getHeader("User-Agent"), req.getHeader("Referer"), req.getRemoteAddr());
        // 302 (not 301) so browsers don't cache the redirect and skip click tracking
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(m.getLongUrl())).build();
    }
}
