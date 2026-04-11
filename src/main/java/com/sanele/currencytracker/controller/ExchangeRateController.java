package com.sanele.currencytracker.controller;

import com.sanele.currencytracker.dto.CompareResponse;
import com.sanele.currencytracker.dto.ConversionResponse;
import com.sanele.currencytracker.dto.HistoryEntry;
import com.sanele.currencytracker.dto.RateResponse;
import com.sanele.currencytracker.service.CurrencyConversionService;
import com.sanele.currencytracker.service.ExchangeRateService;
import com.sanele.currencytracker.service.RateSnapshotService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;
    private final CurrencyConversionService currencyConversionService;
    private final RateSnapshotService rateSnapshotService;

    /**
     * GET /api/rates/{base}
     * Returns current exchange rates for the given base currency.
     * Results are cached for 1 hour.
     *
     * Example: GET /api/rates/USD
     */
    @GetMapping("/{base}")
    public ResponseEntity<RateResponse> getRates(@PathVariable String base) {
        RateResponse response = exchangeRateService.getRates(base);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/rates/convert?from=USD&to=ZAR&amount=100
     * Converts an amount from one currency to another using live rates.
     */
    @GetMapping("/convert")
    public ResponseEntity<ConversionResponse> convert(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam BigDecimal amount
    ) {
        ConversionResponse response = currencyConversionService.convert(from, to, amount);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/rates/history/{base}/{target}?from=2024-01-01&to=2024-03-01
     * Returns historical snapshot data for a currency pair.
     */
    @GetMapping("/history/{base}/{target}")
    public ResponseEntity<List<HistoryEntry>> getHistory(
            @PathVariable String base,
            @PathVariable String target,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        List<HistoryEntry> history = rateSnapshotService.getHistory(base, target, from, to);
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/rates/supported
     * Returns the list of supported currency codes.
     */
    @GetMapping("/supported")
    public ResponseEntity<List<String>> getSupportedCurrencies() {
        return ResponseEntity.ok(exchangeRateService.getSupportedCurrencies());
    }

    /**
     * GET /api/rates/compare?base=USD&targets=EUR,GBP,ZAR,JPY
     * Compares exchange rates for multiple target currencies against a base currency.
     */
    @GetMapping("/compare")
    public ResponseEntity<CompareResponse> compareRates(
            @RequestParam String base,
            @RequestParam String targets
    ) {
        List<String> targetList = Arrays.stream(targets.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        if (targetList.isEmpty()) {
            throw new IllegalArgumentException("At least one target currency must be provided");
        }

        CompareResponse response = exchangeRateService.compareRates(base, targetList);
        return ResponseEntity.ok(response);
    }
}
