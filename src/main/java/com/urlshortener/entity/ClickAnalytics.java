package com.urlshortener.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "click_analytics",
    indexes = {
        @Index(name = "idx_click_short_code", columnList = "short_code"),
        @Index(name = "idx_clicked_at",       columnList = "clicked_at")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, length = 50)
    private String shortCode;

    @Column(name = "clicked_at", nullable = false)
    private LocalDateTime clickedAt;

    /** Visitor IP address (last octet masked when returned via API). */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "referer", length = 500)
    private String referer;

    @PrePersist
    protected void onCreate() {
        this.clickedAt = LocalDateTime.now();
    }
}
