package com.sanele.currencytracker.client;

import com.sanele.currencytracker.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateClient {

    private static final String BASE_URL = "https://open.er-api.com/v6/latest/";

    private final RestTemplate restTemplate;

    /**
     * Fetches live exchange rates for a given base currency from the open.er-api.com free API.
     * No API key required.
     *
     * @param baseCurrency the ISO 4217 currency code (e.g. "USD")
     * @return a map of target currency codes to their exchange rates
     * @throws ExternalApiException if the API call fails or returns an error response
     */
    public Map<String, BigDecimal> fetchRates(String baseCurrency) {
        String url = BASE_URL + baseCurrency.toUpperCase();
        log.info("Fetching exchange rates from external API for base currency: {}", baseCurrency);

        try {
            ExchangeRateApiResponse response = restTemplate.getForObject(url, ExchangeRateApiResponse.class);

            if (response == null) {
                throw new ExternalApiException("Received null response from exchange rate API for base: " + baseCurrency);
            }

            if (!response.isSuccess()) {
                throw new ExternalApiException("Exchange rate API returned non-success result for base: " + baseCurrency
                        + ", result=" + response.result());
            }

            if (response.rates() == null || response.rates().isEmpty()) {
                throw new ExternalApiException("Exchange rate API returned empty rates for base: " + baseCurrency);
            }

            log.info("Successfully fetched {} rates for base currency {}", response.rates().size(), baseCurrency);
            return response.rates();

        } catch (RestClientException e) {
            log.error("HTTP error while calling exchange rate API for base {}: {}", baseCurrency, e.getMessage());
            throw new ExternalApiException("Failed to fetch exchange rates for " + baseCurrency + ": " + e.getMessage(), e);
        }
    }
}
