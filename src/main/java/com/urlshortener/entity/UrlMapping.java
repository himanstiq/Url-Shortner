package com.urlshortener.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "url_mappings",
    indexes = {
        @Index(name = "idx_short_code",   columnList = "short_code"),
        @Index(name = "idx_custom_alias", columnList = "custom_alias")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The code used in the short URL (generated or = customAlias when provided). */
    @Column(name = "short_code", unique = true, nullable = false, length = 50)
    private String shortCode;

    /** The destination URL that the short code resolves to. */
    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    /** Non-null when the user supplied a human-readable alias. Equals shortCode in that case. */
    @Column(name = "custom_alias", unique = true, length = 50)
    private String customAlias;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Null means the URL never expires. */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(name = "click_count", nullable = false)
    private Long clickCount = 0L;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /** Stored for rate-limiting / abuse-tracking (hashed in a real deployment). */
    @Column(name = "created_by_ip", length = 45)
    private String createdByIp;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
