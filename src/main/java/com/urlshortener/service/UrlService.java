package com.urlshortener.service;

import com.urlshortener.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UrlService {

    /**
     * Creates a new short URL mapping.
     *
     * @param request   validated request body
     * @param ipAddress caller's IP (for abuse tracking)
     * @return the created mapping with its generated short URL
     */
    ShortenResponse shortenUrl(ShortenRequest request, String ipAddress);

    /**
     * Resolves a short code to its original URL.
     * Side-effects: increments click count, persists analytics row.
     *
     * @throws com.urlshortener.exception.UrlNotFoundException  if the code doesn't exist or is inactive
     * @throws com.urlshortener.exception.UrlExpiredException   if the link has passed its expiry date
     */
    String getOriginalUrl(String shortCode, String ipAddress, String userAgent, String referer);

    /**
     * Returns the full statistics for a short URL including recent click history.
     */
    UrlStatsResponse getStats(String shortCode);

    /**
     * Permanently deletes a short URL and its associated analytics.
     */
    void deleteUrl(String shortCode);

    /**
     * Returns a paginated list of all short URLs, newest first.
     */
    Page<ShortenResponse> getAllUrls(Pageable pageable);

    /**
     * Updates mutable fields of an existing short URL.
     * Any null field in {@code request} is left unchanged.
     */
    ShortenResponse updateUrl(String shortCode, UrlUpdateRequest request);
}
