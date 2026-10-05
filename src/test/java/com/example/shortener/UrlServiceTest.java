package com.example.shortener;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.example.shortener.dto.Dtos.StatsResponse;
import com.example.shortener.model.UrlMapping;
import com.example.shortener.repo.*;
import com.example.shortener.service.UrlService;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {
    static final Instant NOW = Instant.parse("2026-09-30T12:00:00Z");
    @Mock UrlMappingRepository urls;
    @Mock ClickEventRepository clicks;
    UrlService service;

    @BeforeEach void setUp() { service = new UrlService(urls, clicks, Clock.fixed(NOW, ZoneOffset.UTC)); }

    static HttpStatus statusOf(Executable e) {
        return (HttpStatus) assertThrows(ResponseStatusException.class, e).getStatusCode();
    }

    // ---- create ----
    @Test void createsMappingWithGeneratedCode() {
        when(urls.existsByShortCode(anyString())).thenReturn(false);
        when(urls.save(any(UrlMapping.class))).thenAnswer(i -> i.getArgument(0));
        UrlMapping m = service.create("  https://example.com/path?q=1  ", null);
        assertEquals("https://example.com/path?q=1", m.getLongUrl());
        assertTrue(m.getShortCode().matches("[0-9A-Za-z]{7}"));
        assertEquals(NOW, m.getCreatedAt());
        assertNull(m.getExpiresAt());
    }
    @Test void setsExpiryFromTtl() {
        when(urls.existsByShortCode(anyString())).thenReturn(false);
        when(urls.save(any(UrlMapping.class))).thenAnswer(i -> i.getArgument(0));
        assertEquals(NOW.plus(Duration.ofDays(3)), service.create("http://a.com", 3).getExpiresAt());
    }
    @Test void retriesOnCollision() {
        when(urls.existsByShortCode(anyString())).thenReturn(true, true, false);
        when(urls.save(any(UrlMapping.class))).thenAnswer(i -> i.getArgument(0));
        assertNotNull(service.create("https://a.com", null));
        verify(urls, times(3)).existsByShortCode(anyString());
    }
    @Test void failsAfterTooManyCollisions() {
        when(urls.existsByShortCode(anyString())).thenReturn(true);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, statusOf(() -> service.create("https://a.com", null)));
        verify(urls, never()).save(any());
    }
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "example.com", "ftp://example.com", "javascript:alert(1)", "https://", "http://bad url.com"})
    void rejectsInvalidUrls(String url) {
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.create(url, null)));
        verifyNoInteractions(urls);
    }
    @Test void rejectsTooLongUrl() {
        String url = "https://example.com/" + "a".repeat(2048);
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.create(url, null)));
    }
    @ParameterizedTest @ValueSource(ints = {0, -1})
    void rejectsNonPositiveTtl(int ttl) {
        assertEquals(HttpStatus.BAD_REQUEST, statusOf(() -> service.create("https://a.com", ttl)));
    }

    // ---- resolve ----
    @Test void resolvesExistingCode() {
        UrlMapping m = new UrlMapping("abc1234", "https://a.com", NOW, null);
        when(urls.findByShortCode("abc1234")).thenReturn(Optional.of(m));
        assertSame(m, service.resolve("abc1234"));
    }
    @Test void unknownCodeIs404() {
        when(urls.findByShortCode("nope")).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, statusOf(() -> service.resolve("nope")));
    }
    @Test void expiredLinkIs410() {
        when(urls.findByShortCode("old")).thenReturn(Optional.of(new UrlMapping("old", "https://a.com", NOW.minusSeconds(100), NOW.minusSeconds(1))));
        assertEquals(HttpStatus.GONE, statusOf(() -> service.resolve("old")));
    }
    @Test void linkExpiringExactlyNowIsExpired() {
        when(urls.findByShortCode("edge")).thenReturn(Optional.of(new UrlMapping("edge", "https://a.com", NOW.minusSeconds(100), NOW)));
        assertEquals(HttpStatus.GONE, statusOf(() -> service.resolve("edge")));
    }
    @Test void notYetExpiredLinkResolves() {
        UrlMapping m = new UrlMapping("ok", "https://a.com", NOW, NOW.plusSeconds(60));
        when(urls.findByShortCode("ok")).thenReturn(Optional.of(m));
        assertSame(m, service.resolve("ok"));
    }

    // ---- stats ----
    @Test void statsGroupsClicksByUtcDay() {
        UrlMapping m = new UrlMapping("abc1234", "https://a.com", NOW, null);
        ReflectionTestUtils.setField(m, "id", 1L);
        when(urls.findByShortCode("abc1234")).thenReturn(Optional.of(m));
        when(clicks.findClickTimes(1L)).thenReturn(List.of(
                Instant.parse("2026-09-29T23:59:59Z"), Instant.parse("2026-09-30T00:00:00Z"), Instant.parse("2026-09-30T10:00:00Z")));
        StatsResponse s = service.stats("abc1234");
        assertEquals(3, s.totalClicks());
        assertEquals(Map.of(LocalDate.of(2026, 9, 29), 1L, LocalDate.of(2026, 9, 30), 2L), s.clicksByDay());
    }
    @Test void statsWithNoClicks() {
        UrlMapping m = new UrlMapping("abc1234", "https://a.com", NOW, null);
        ReflectionTestUtils.setField(m, "id", 1L);
        when(urls.findByShortCode("abc1234")).thenReturn(Optional.of(m));
        when(clicks.findClickTimes(1L)).thenReturn(List.of());
        StatsResponse s = service.stats("abc1234");
        assertEquals(0, s.totalClicks());
        assertTrue(s.clicksByDay().isEmpty());
    }
    @Test void statsForUnknownCodeIs404() {
        when(urls.findByShortCode("x")).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, statusOf(() -> service.stats("x")));
    }
}
