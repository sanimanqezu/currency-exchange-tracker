package com.sanele.currencytracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Single historical rate entry for GET /api/rates/history/{base}/{target}
 */
public record HistoryEntry(
        String baseCurrency,
        String targetCurrency,
        BigDecimal rate,
        LocalDate date
) {}
