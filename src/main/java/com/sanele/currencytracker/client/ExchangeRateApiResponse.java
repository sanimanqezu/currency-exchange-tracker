package com.sanele.currencytracker.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Maps the JSON response from https://open.er-api.com/v6/latest/{base}
 * Example response:
 * {
 *   "result": "success",
 *   "base_code": "USD",
 *   "rates": { "EUR": 0.92, "ZAR": 18.5, ... }
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExchangeRateApiResponse(

        @JsonProperty("result")
        String result,

        @JsonProperty("base_code")
        String baseCode,

        @JsonProperty("time_last_update_utc")
        String timeLastUpdateUtc,

        @JsonProperty("rates")
        Map<String, BigDecimal> rates
) {
    public boolean isSuccess() {
        return "success".equalsIgnoreCase(result);
    }
}
