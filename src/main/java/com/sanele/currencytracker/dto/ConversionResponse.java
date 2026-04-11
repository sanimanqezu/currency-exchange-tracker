package com.sanele.currencytracker.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for GET /api/rates/convert
 */
public record ConversionResponse(
        String fromCurrency,
        String toCurrency,
        BigDecimal amount,
        BigDecimal convertedAmount,
        BigDecimal exchangeRate,
        LocalDateTime rateTimestamp
) {}
