package com.urlshortener.controller;

import com.urlshortener.dto.*;
import com.urlshortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for URL management.
 *
 * <pre>
 * POST   /api/shorten                  – create a short URL
 * GET    /api/stats/{shortCode}        – fetch statistics + recent visits
 * GET    /api/urls                     – paginated list of all URLs
 * PATCH  /api/urls/{shortCode}         – update target / active flag / expiry
 * DELETE /api/urls/{shortCode}         – permanently delete
 * </pre>
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    // ── POST /api/shorten ─────────────────────────────────────────────────────

    /**
     * Shorten a URL, optionally supplying a custom alias and/or expiry.
     *
     * <pre>
     * {
     *   "originalUrl"   : "https://very-long-domain.com/path?query=value",
     *   "customAlias"   : "my-link",   // optional
     *   "expiresInDays" : 30           // optional
     * }
     * </pre>
     */
    @PostMapping("/shorten")
    public ResponseEntity<ApiResponse<ShortenResponse>> shorten(
            @Valid @RequestBody ShortenRequest request,
            HttpServletRequest httpRequest) {

        ShortenResponse response = urlService.shortenUrl(request, extractClientIp(httpRequest));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("URL shortened successfully", response));
    }

    // ── GET /api/stats/{shortCode} ────────────────────────────────────────────

    /**
     * Returns full statistics for a short URL, including the 20 most recent visits.
     */
    @GetMapping("/stats/{shortCode}")
    public ResponseEntity<ApiResponse<UrlStatsResponse>> getStats(
            @PathVariable String shortCode) {

        UrlStatsResponse stats = urlService.getStats(shortCode);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // ── GET /api/urls ─────────────────────────────────────────────────────────

    /**
     * Returns a paginated list of all short URLs.
     *
     * <p>Query parameters:
     * <ul>
     *   <li>{@code page}      – zero-based page index (default 0)</li>
     *   <li>{@code size}      – items per page, capped at 100 (default 10)</li>
     *   <li>{@code sortBy}    – field to sort on (default "createdAt")</li>
     *   <li>{@code direction} – "asc" or "desc" (default "desc")</li>
     * </ul>
     */
    @GetMapping("/urls")
    public ResponseEntity<ApiResponse<Page<ShortenResponse>>> getAllUrls(
            @RequestParam(defaultValue = "0")         int    page,
            @RequestParam(defaultValue = "10")        int    size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String direction) {

        Sort.Direction dir = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        PageRequest pageRequest = PageRequest.of(
                page,
                Math.min(size, 100),   // hard cap – prevent enormous responses
                Sort.by(dir, sortBy)
        );

        Page<ShortenResponse> result = urlService.getAllUrls(pageRequest);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── PATCH /api/urls/{shortCode} ───────────────────────────────────────────

    /**
     * Updates one or more mutable fields of an existing short URL.
     * All fields are optional; omit any field to leave it unchanged.
     *
     * <pre>
     * { "isActive": false }                      // deactivate
     * { "isActive": true  }                      // re-activate
     * { "originalUrl": "https://new-target.com"} // change destination
     * { "expiresInDays": 7  }                    // extend expiry 7 days from now
     * { "expiresInDays": 0  }                    // remove expiry (permanent)
     * </pre>
     */
    @PatchMapping("/urls/{shortCode}")
    public ResponseEntity<ApiResponse<ShortenResponse>> updateUrl(
            @PathVariable String shortCode,
            @Valid @RequestBody UrlUpdateRequest request) {

        ShortenResponse updated = urlService.updateUrl(shortCode, request);
        return ResponseEntity.ok(ApiResponse.success("URL updated successfully", updated));
    }

    // ── DELETE /api/urls/{shortCode} ──────────────────────────────────────────

    /**
     * Permanently deletes a short URL and all its click analytics.
     */
    @DeleteMapping("/urls/{shortCode}")
    public ResponseEntity<ApiResponse<Void>> deleteUrl(@PathVariable String shortCode) {
        urlService.deleteUrl(shortCode);
        return ResponseEntity.ok(ApiResponse.success("URL deleted successfully", null));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
