package com.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UrlStatsResponse {

    private Long          id;
    private String        shortCode;
    private String        shortUrl;
    private String        originalUrl;
    private String        customAlias;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Long          clickCount;
    private Boolean       isActive;

    /** Last 20 visits, newest first. */
    private List<ClickAnalyticsDto> recentClicks;
}
