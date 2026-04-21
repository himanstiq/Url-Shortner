package com.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShortenResponse {

    private Long          id;
    private String        shortCode;
    /** Fully-qualified short URL ready to share, e.g. http://localhost:8080/abc123 */
    private String        shortUrl;
    private String        originalUrl;
    /** Only present when a custom alias was requested. */
    private String        customAlias;
    private LocalDateTime createdAt;
    /** Null when the link has no expiry. */
    private LocalDateTime expiresAt;
    private Long          clickCount;
    private Boolean       isActive;
}
