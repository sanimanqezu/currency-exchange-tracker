package com.sanele.currencytracker.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for GET /api/rates/compare
 */
public record CompareResponse(
        String baseCurrency,
        Map<String, BigDecimal> rates,
        LocalDateTime fetchedAt
) {}
