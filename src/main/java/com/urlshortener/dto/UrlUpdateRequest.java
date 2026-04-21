package com.urlshortener.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class UrlUpdateRequest {

    /** Set to false to deactivate (visitors get 404); true to re-activate. */
    private Boolean isActive;

    /** Replace the destination URL. */
    @URL(message = "originalUrl must be a valid URL")
    private String originalUrl;

    /**
     * Adjust expiry from now.
     * 0 = remove expiry (make permanent).
     * null = leave expiry unchanged.
     */
    @Min(value = 0,    message = "expiresInDays cannot be negative")
    @Max(value = 3650, message = "expiresInDays cannot exceed 3650")
    private Integer expiresInDays;
}
