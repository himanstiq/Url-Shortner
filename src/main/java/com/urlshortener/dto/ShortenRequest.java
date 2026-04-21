package com.urlshortener.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class ShortenRequest {

    @NotBlank(message = "originalUrl must not be blank")
    @URL(message = "originalUrl must be a valid URL starting with http:// or https://")
    private String originalUrl;

    /**
     * Optional human-readable alias.
     * Must be 3–30 characters; letters, digits, hyphens, underscores only.
     */
    @Pattern(
        regexp  = "^[a-zA-Z0-9_-]{3,30}$",
        message = "customAlias must be 3–30 characters and contain only letters, digits, '-', or '_'"
    )
    private String customAlias;

    /**
     * How many days until the link expires.
     * Omit (or set null) for a permanent link.
     */
    @Min(value = 1,    message = "expiresInDays must be at least 1")
    @Max(value = 3650, message = "expiresInDays cannot exceed 3650 (10 years)")
    private Integer expiresInDays;
}
