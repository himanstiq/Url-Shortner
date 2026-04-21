package com.urlshortener.repository;

import com.urlshortener.entity.ClickAnalytics;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClickAnalyticsRepository extends JpaRepository<ClickAnalytics, Long> {

    /**
     * Returns the most recent clicks for a given code, newest first.
     * Pass {@code PageRequest.of(0, 20)} to cap at 20 results.
     */
    List<ClickAnalytics> findByShortCodeOrderByClickedAtDesc(String shortCode, Pageable pageable);

    long countByShortCode(String shortCode);

    /** Called when a short URL is deleted so orphan rows are removed too. */
    void deleteByShortCode(String shortCode);
}
