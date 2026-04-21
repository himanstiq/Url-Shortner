package com.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClickAnalyticsDto {

    private LocalDateTime clickedAt;
    /** Last octet masked for privacy, e.g. "192.168.1.xxx" */
    private String        ipAddress;
    private String        userAgent;
    private String        referer;
}
