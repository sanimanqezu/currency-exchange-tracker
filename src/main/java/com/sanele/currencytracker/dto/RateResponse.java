package com.sanele.currencytracker.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for GET /api/rates/{base}
 */
public record RateResponse(
        String baseCurrency,
        Map<String, BigDecimal> rates,
        LocalDateTime fetchedAt
) {}
