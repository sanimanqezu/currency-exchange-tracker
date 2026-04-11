package com.sanele.currencytracker.service;

import com.sanele.currencytracker.client.ExchangeRateClient;
import com.sanele.currencytracker.config.CacheConfig;
import com.sanele.currencytracker.dto.CompareResponse;
import com.sanele.currencytracker.dto.RateResponse;
import com.sanele.currencytracker.entity.ExchangeRate;
import com.sanele.currencytracker.exception.CurrencyNotFoundException;
import com.sanele.currencytracker.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

    /**
     * Major currencies supported by this application.
     */
    public static final List<String> SUPPORTED_CURRENCIES = List.of(
            "USD", "EUR", "GBP", "ZAR", "JPY", "CAD", "AUD", "CHF",
            "CNY", "INR", "MXN", "BRL", "SGD", "HKD", "NOK", "SEK",
            "DKK", "NZD", "KRW", "TRY"
    );

    private final ExchangeRateClient exchangeRateClient;
    private final ExchangeRateRepository exchangeRateRepository;

    /**
     * Fetches live rates for a base currency, caching results for 1 hour.
     * Result is also persisted to the database.
     *
     * @param baseCurrency the base currency code (e.g. "USD")
     * @return RateResponse containing all rates
     */
    @Cacheable(value = CacheConfig.RATES_CACHE, key = "#baseCurrency.toUpperCase()")
    @Transactional
    public RateResponse getRates(String baseCurrency) {
        String base = baseCurrency.toUpperCase();
        validateCurrency(base);

        log.info("Cache miss — fetching rates from external API for base: {}", base);
        Map<String, BigDecimal> rates = exchangeRateClient.fetchRates(base);

        LocalDateTime now = LocalDateTime.now();
        persistExchangeRates(base, rates, now);

        return new RateResponse(base, rates, now);
    }

    /**
     * Evicts the cache entry for a given base currency and re-fetches.
     */
    @CacheEvict(value = CacheConfig.RATES_CACHE, key = "#baseCurrency.toUpperCase()")
    @Transactional
    public RateResponse refreshRates(String baseCurrency) {
        log.info("Evicting cache and refreshing rates for base: {}", baseCurrency);
        return getRates(baseCurrency);
    }

    /**
     * Compares rates for selected target currencies against a base currency.
     */
    public CompareResponse compareRates(String baseCurrency, List<String> targets) {
        String base = baseCurrency.toUpperCase();
        validateCurrency(base);

        RateResponse allRates = getRates(base);

        Map<String, BigDecimal> filtered = targets.stream()
                .map(String::toUpperCase)
                .filter(t -> {
                    if (!allRates.rates().containsKey(t)) {
                        log.warn("Target currency '{}' not found in rates, skipping", t);
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toMap(
                        t -> t,
                        t -> allRates.rates().get(t),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        return new CompareResponse(base, filtered, allRates.fetchedAt());
    }

    /**
     * Returns the list of supported currency codes.
     */
    public List<String> getSupportedCurrencies() {
        return SUPPORTED_CURRENCIES;
    }

    /**
     * Fetches rates without caching — used internally by the scheduler.
     */
    @Transactional
    public Map<String, BigDecimal> fetchAndPersistRates(String baseCurrency) {
        String base = baseCurrency.toUpperCase();
        Map<String, BigDecimal> rates = exchangeRateClient.fetchRates(base);
        persistExchangeRates(base, rates, LocalDateTime.now());
        return rates;
    }

    private void persistExchangeRates(String base, Map<String, BigDecimal> rates, LocalDateTime fetchedAt) {
        List<ExchangeRate> entities = rates.entrySet().stream()
                .map(entry -> ExchangeRate.builder()
                        .baseCurrency(base)
                        .targetCurrency(entry.getKey())
                        .rate(entry.getValue())
                        .fetchedAt(fetchedAt)
                        .build())
                .collect(Collectors.toList());

        exchangeRateRepository.saveAll(entities);
        log.debug("Persisted {} exchange rate records for base {}", entities.size(), base);
    }

    private void validateCurrency(String currency) {
        if (!SUPPORTED_CURRENCIES.contains(currency)) {
            throw new CurrencyNotFoundException(
                    "Currency '" + currency + "' is not supported. Call /api/rates/supported for the full list.");
        }
    }
}
