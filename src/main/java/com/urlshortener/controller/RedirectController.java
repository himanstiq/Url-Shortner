package com.urlshortener.controller;

import com.urlshortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles the core redirect: {@code GET /{shortCode}} → {@code 302 Found}.
 * Kept in its own controller to avoid the "/api" prefix.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class RedirectController {

    private final UrlService urlService;

    @GetMapping("/{shortCode:[a-zA-Z0-9_-]+}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode,
            HttpServletRequest request) {

        String ip        = extractClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String referer   = request.getHeader("Referer");

        String originalUrl = urlService.getOriginalUrl(shortCode, ip, userAgent, referer);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, originalUrl)
                // Cache-Control: no-store prevents browsers caching the 302
                // (important for analytics accuracy and for expiry/deactivation to take effect)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }

    /**
     * Extracts the real client IP, respecting common reverse-proxy headers.
     * Priority: X-Forwarded-For → X-Real-IP → socket remote address.
     */
    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // X-Forwarded-For can be a comma-separated chain; first entry is the originating client
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) {
            return xri.trim();
        }
        return request.getRemoteAddr();
    }
}
