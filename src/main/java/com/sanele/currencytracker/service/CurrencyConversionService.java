package com.sanele.currencytracker.service;

import com.sanele.currencytracker.dto.ConversionResponse;
import com.sanele.currencytracker.dto.RateResponse;
import com.sanele.currencytracker.exception.CurrencyNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyConversionService {

    private final ExchangeRateService exchangeRateService;

    /**
     * Converts a given amount from one currency to another using live exchange rates.
     *
     * @param fromCurrency source currency code (e.g. "USD")
     * @param toCurrency   target currency code (e.g. "ZAR")
     * @param amount       the amount to convert
     * @return ConversionResponse with the converted amount and rate used
     */
    public ConversionResponse convert(String fromCurrency, String toCurrency, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be a positive number");
        }

        String from = fromCurrency.toUpperCase();
        String to = toCurrency.toUpperCase();

        log.info("Converting {} {} to {}", amount, from, to);

        // Fetch rates with base = fromCurrency (cached)
        RateResponse rateResponse = exchangeRateService.getRates(from);

        BigDecimal exchangeRate = rateResponse.rates().get(to);
        if (exchangeRate == null) {
            throw new CurrencyNotFoundException(
                    "Target currency '" + to + "' is not available in the rates for base '" + from + "'");
        }

        BigDecimal converted = amount.multiply(exchangeRate).setScale(4, RoundingMode.HALF_UP);

        return new ConversionResponse(
                from,
                to,
                amount,
                converted,
                exchangeRate,
                rateResponse.fetchedAt()
        );
    }
}
