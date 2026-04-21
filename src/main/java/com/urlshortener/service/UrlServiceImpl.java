package com.urlshortener.service;

import com.urlshortener.dto.*;
import com.urlshortener.entity.ClickAnalytics;
import com.urlshortener.entity.UrlMapping;
import com.urlshortener.exception.CustomAliasAlreadyExistsException;
import com.urlshortener.exception.UrlExpiredException;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.repository.ClickAnalyticsRepository;
import com.urlshortener.repository.UrlMappingRepository;
import com.urlshortener.util.Base62Encoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlServiceImpl implements UrlService {

    private final UrlMappingRepository    urlMappingRepository;
    private final ClickAnalyticsRepository clickAnalyticsRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // ── Public API ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ShortenResponse shortenUrl(ShortenRequest request, String ipAddress) {
        String shortCode;
        String customAlias = null;

        if (StringUtils.hasText(request.getCustomAlias())) {
            String alias = request.getCustomAlias().trim();
            if (urlMappingRepository.existsByShortCode(alias)) {
                throw new CustomAliasAlreadyExistsException(
                        "Custom alias '" + alias + "' is already taken. Please choose a different one.");
            }
            shortCode   = alias;
            customAlias = alias;
        } else {
            shortCode = generateUniqueShortCode();
        }

        LocalDateTime expiresAt = null;
        if (request.getExpiresInDays() != null && request.getExpiresInDays() > 0) {
            expiresAt = LocalDateTime.now().plusDays(request.getExpiresInDays());
        }

        UrlMapping mapping = UrlMapping.builder()
                .shortCode(shortCode)
                .originalUrl(request.getOriginalUrl().trim())
                .customAlias(customAlias)
                .expiresAt(expiresAt)
                .clickCount(0L)
                .isActive(true)
                .createdByIp(ipAddress)
                .build();

        UrlMapping saved = urlMappingRepository.save(mapping);
        log.info("Created short URL [{}] → [{}]", shortCode, request.getOriginalUrl());
        return toShortenResponse(saved);
    }

    @Override
    @Transactional
    public String getOriginalUrl(String shortCode, String ipAddress, String userAgent, String referer) {
        UrlMapping mapping = findActiveMapping(shortCode);

        // Atomic counter increment — no dirty-read risk
        urlMappingRepository.incrementClickCount(shortCode);

        // Record visit (best-effort; failures don't break the redirect)
        try {
            ClickAnalytics analytics = ClickAnalytics.builder()
                    .shortCode(shortCode)
                    .ipAddress(truncate(ipAddress, 45))
                    .userAgent(truncate(userAgent, 500))
                    .referer(truncate(referer, 500))
                    .build();
            clickAnalyticsRepository.save(analytics);
        } catch (Exception e) {
            log.warn("Analytics save failed for [{}]: {}", shortCode, e.getMessage());
        }

        log.info("Redirect [{}] → [{}]", shortCode, mapping.getOriginalUrl());
        return mapping.getOriginalUrl();
    }

    @Override
    @Transactional(readOnly = true)
    public UrlStatsResponse getStats(String shortCode) {
        UrlMapping mapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("No URL found for: /" + shortCode));

        List<ClickAnalytics> rawClicks = clickAnalyticsRepository
                .findByShortCodeOrderByClickedAtDesc(shortCode, PageRequest.of(0, 20));

        List<ClickAnalyticsDto> recentClicks = rawClicks.stream()
                .map(c -> ClickAnalyticsDto.builder()
                        .clickedAt(c.getClickedAt())
                        .ipAddress(maskIp(c.getIpAddress()))
                        .userAgent(c.getUserAgent())
                        .referer(c.getReferer())
                        .build())
                .collect(Collectors.toList());

        return UrlStatsResponse.builder()
                .id(mapping.getId())
                .shortCode(mapping.getShortCode())
                .shortUrl(baseUrl + "/" + mapping.getShortCode())
                .originalUrl(mapping.getOriginalUrl())
                .customAlias(mapping.getCustomAlias())
                .createdAt(mapping.getCreatedAt())
                .expiresAt(mapping.getExpiresAt())
                .clickCount(mapping.getClickCount())
                .isActive(mapping.getIsActive())
                .recentClicks(recentClicks)
                .build();
    }

    @Override
    @Transactional
    public void deleteUrl(String shortCode) {
        UrlMapping mapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("No URL found for: /" + shortCode));

        clickAnalyticsRepository.deleteByShortCode(shortCode);
        urlMappingRepository.delete(mapping);
        log.info("Deleted short URL: [{}]", shortCode);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShortenResponse> getAllUrls(Pageable pageable) {
        return urlMappingRepository.findAll(pageable).map(this::toShortenResponse);
    }

    @Override
    @Transactional
    public ShortenResponse updateUrl(String shortCode, UrlUpdateRequest request) {
        UrlMapping mapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("No URL found for: /" + shortCode));

        if (request.getIsActive() != null) {
            mapping.setIsActive(request.getIsActive());
        }
        if (StringUtils.hasText(request.getOriginalUrl())) {
            mapping.setOriginalUrl(request.getOriginalUrl().trim());
        }
        if (request.getExpiresInDays() != null) {
            mapping.setExpiresAt(
                    request.getExpiresInDays() == 0
                    ? null                                                          // remove expiry
                    : LocalDateTime.now().plusDays(request.getExpiresInDays())     // set new expiry
            );
        }

        UrlMapping saved = urlMappingRepository.save(mapping);
        log.info("Updated short URL: [{}]", shortCode);
        return toShortenResponse(saved);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Looks up a mapping and validates it is active and not expired.
     * Used by the redirect path only.
     */
    private UrlMapping findActiveMapping(String shortCode) {
        UrlMapping mapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("No URL found for: /" + shortCode));

        if (Boolean.FALSE.equals(mapping.getIsActive())) {
            throw new UrlNotFoundException("This short URL has been deactivated.");
        }
        if (mapping.getExpiresAt() != null && LocalDateTime.now().isAfter(mapping.getExpiresAt())) {
            throw new UrlExpiredException("This short URL expired on " + mapping.getExpiresAt() + ".");
        }
        return mapping;
    }

    /**
     * Generates a 6-char Base62 code that doesn't already exist in the DB.
     * Falls back to 8-char codes after 10 failed attempts (astronomically unlikely).
     */
    private String generateUniqueShortCode() {
        for (int attempt = 1; attempt <= 10; attempt++) {
            String code = Base62Encoder.generateRandom();
            if (!urlMappingRepository.existsByShortCode(code)) {
                return code;
            }
        }
        // Collision storm fallback — extends to 8 chars
        String longCode = Base62Encoder.generateRandom(8);
        log.warn("Short code collision storm — generated 8-char fallback: {}", longCode);
        return longCode;
    }

    private ShortenResponse toShortenResponse(UrlMapping m) {
        return ShortenResponse.builder()
                .id(m.getId())
                .shortCode(m.getShortCode())
                .shortUrl(baseUrl + "/" + m.getShortCode())
                .originalUrl(m.getOriginalUrl())
                .customAlias(m.getCustomAlias())
                .createdAt(m.getCreatedAt())
                .expiresAt(m.getExpiresAt())
                .clickCount(m.getClickCount())
                .isActive(m.getIsActive())
                .build();
    }

    /** Masks the last IPv4 octet for privacy: "192.168.1.42" → "192.168.1.xxx" */
    private String maskIp(String ip) {
        if (!StringUtils.hasText(ip)) return null;
        int lastDot = ip.lastIndexOf('.');
        return lastDot > 0 ? ip.substring(0, lastDot) + ".xxx" : "xxx";
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
