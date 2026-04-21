package com.urlshortener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.dto.ShortenRequest;
import com.urlshortener.dto.UrlUpdateRequest;
import com.urlshortener.repository.ClickAnalyticsRepository;
import com.urlshortener.repository.UrlMappingRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UrlShortenerApplicationTests {

    @Autowired MockMvc             mvc;
    @Autowired ObjectMapper        om;
    @Autowired UrlMappingRepository urlRepo;
    @Autowired ClickAnalyticsRepository clickRepo;

    /** Short code created in the first test, reused by subsequent ones. */
    static String createdShortCode;
    static String customAliasCode = "test-alias";

    @BeforeEach
    void cleanDb() {
        // Only wipe before the very first test
        if (createdShortCode == null && !urlRepo.existsByShortCode(customAliasCode)) {
            clickRepo.deleteAll();
            urlRepo.deleteAll();
        }
    }

    // ── POST /api/shorten ─────────────────────────────────────────────────────

    @Test @Order(1)
    void shorten_validUrl_returns201AndShortCode() throws Exception {
        ShortenRequest req = new ShortenRequest();
        req.setOriginalUrl("https://www.example.com/some/very/long/path?foo=bar");

        MvcResult result = mvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.shortCode").isNotEmpty())
                .andExpect(jsonPath("$.data.shortUrl").value(containsString("localhost")))
                .andExpect(jsonPath("$.data.originalUrl").value("https://www.example.com/some/very/long/path?foo=bar"))
                .andExpect(jsonPath("$.data.clickCount").value(0))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andReturn();

        createdShortCode = om.readTree(result.getResponse().getContentAsString())
                             .at("/data/shortCode").asText();
    }

    @Test @Order(2)
    void shorten_withCustomAlias_usesAlias() throws Exception {
        ShortenRequest req = new ShortenRequest();
        req.setOriginalUrl("https://www.google.com");
        req.setCustomAlias(customAliasCode);

        mvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.shortCode").value(customAliasCode))
                .andExpect(jsonPath("$.data.customAlias").value(customAliasCode));
    }

    @Test @Order(3)
    void shorten_duplicateCustomAlias_returns409() throws Exception {
        ShortenRequest req = new ShortenRequest();
        req.setOriginalUrl("https://www.bing.com");
        req.setCustomAlias(customAliasCode);

        mvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test @Order(4)
    void shorten_blankUrl_returns400() throws Exception {
        mvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test @Order(5)
    void shorten_invalidUrl_returns400() throws Exception {
        mvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\":\"not-a-valid-url\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @Order(6)
    void shorten_missingBody_returns400() throws Exception {
        mvc.perform(post("/api/shorten").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test @Order(7)
    void shorten_customAliasTooShort_returns400() throws Exception {
        ShortenRequest req = new ShortenRequest();
        req.setOriginalUrl("https://www.example.com");
        req.setCustomAlias("ab");   // < 3 chars

        mvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── GET /{shortCode} ──────────────────────────────────────────────────────

    @Test @Order(8)
    void redirect_validCode_returns302() throws Exception {
        mvc.perform(get("/" + createdShortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        "https://www.example.com/some/very/long/path?foo=bar"));
    }

    @Test @Order(9)
    void redirect_unknownCode_returns404() throws Exception {
        mvc.perform(get("/ZZZZZZ"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/stats ────────────────────────────────────────────────────────

    @Test @Order(10)
    void stats_afterOneClick_showsClickCount() throws Exception {
        mvc.perform(get("/api/stats/" + createdShortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clickCount").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.recentClicks").isArray());
    }

    @Test @Order(11)
    void stats_unknownCode_returns404() throws Exception {
        mvc.perform(get("/api/stats/doesnotexist"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/urls ─────────────────────────────────────────────────────────

    @Test @Order(12)
    void getAllUrls_defaultPagination_returnsList() throws Exception {
        mvc.perform(get("/api/urls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThan(0))));
    }

    @Test @Order(13)
    void getAllUrls_pageSizeOne_returnsOnlyOneItem() throws Exception {
        mvc.perform(get("/api/urls?page=0&size=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.totalElements").value(greaterThanOrEqualTo(1)));
    }

    // ── PATCH /api/urls/{shortCode} ───────────────────────────────────────────

    @Test @Order(14)
    void updateUrl_deactivate_returns200() throws Exception {
        UrlUpdateRequest req = new UrlUpdateRequest();
        req.setIsActive(false);

        mvc.perform(patch("/api/urls/" + createdShortCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test @Order(15)
    void redirect_deactivatedUrl_returns404() throws Exception {
        mvc.perform(get("/" + createdShortCode))
                .andExpect(status().isNotFound());
    }

    @Test @Order(16)
    void updateUrl_reactivate_returns200() throws Exception {
        UrlUpdateRequest req = new UrlUpdateRequest();
        req.setIsActive(true);

        mvc.perform(patch("/api/urls/" + createdShortCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test @Order(17)
    void updateUrl_changeOriginalUrl_returnsUpdated() throws Exception {
        UrlUpdateRequest req = new UrlUpdateRequest();
        req.setOriginalUrl("https://www.updated-example.com");

        mvc.perform(patch("/api/urls/" + createdShortCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.originalUrl").value("https://www.updated-example.com"));
    }

    @Test @Order(18)
    void updateUrl_unknownCode_returns404() throws Exception {
        UrlUpdateRequest req = new UrlUpdateRequest();
        req.setIsActive(false);

        mvc.perform(patch("/api/urls/UNKNOWN99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/urls/{shortCode} ──────────────────────────────────────────

    @Test @Order(19)
    void deleteUrl_existing_returns200() throws Exception {
        mvc.perform(delete("/api/urls/" + createdShortCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test @Order(20)
    void redirect_afterDelete_returns404() throws Exception {
        mvc.perform(get("/" + createdShortCode))
                .andExpect(status().isNotFound());
    }

    @Test @Order(21)
    void deleteUrl_nonExistent_returns404() throws Exception {
        mvc.perform(delete("/api/urls/ghost-link"))
                .andExpect(status().isNotFound());
    }

    // ── Expiry ────────────────────────────────────────────────────────────────

    @Test @Order(22)
    void shorten_withExpiryDays_persistsExpiry() throws Exception {
        ShortenRequest req = new ShortenRequest();
        req.setOriginalUrl("https://www.temporal.com");
        req.setExpiresInDays(7);

        mvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.expiresAt").isNotEmpty());
    }
}
