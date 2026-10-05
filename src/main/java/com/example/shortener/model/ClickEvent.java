package com.example.shortener.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "click_event", indexes = @Index(name = "idx_click_url_time", columnList = "url_mapping_id, clicked_at"))
public class ClickEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "url_mapping_id")
    private UrlMapping urlMapping;
    @Column(name = "clicked_at", nullable = false)
    private Instant clickedAt;
    @Column(name = "user_agent", length = 512)
    private String userAgent;
    @Column(length = 2048)
    private String referrer;
    @Column(name = "ip_hash", length = 64)
    private String ipHash;

    protected ClickEvent() {}
    public ClickEvent(UrlMapping urlMapping, Instant clickedAt, String userAgent, String referrer, String ipHash) {
        this.urlMapping = urlMapping; this.clickedAt = clickedAt; this.userAgent = userAgent;
        this.referrer = referrer; this.ipHash = ipHash;
    }
    public Long getId() { return id; }
    public Instant getClickedAt() { return clickedAt; }
}
