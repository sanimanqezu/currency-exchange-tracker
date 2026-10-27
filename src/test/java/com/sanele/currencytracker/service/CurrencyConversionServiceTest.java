package com.sanele.currencytracker.service;

import com.sanele.currencytracker.dto.ConversionResponse;
import com.sanele.currencytracker.dto.RateResponse;
import com.sanele.currencytracker.exception.CurrencyNotFoundException;
import com.sanele.currencytracker.exception.ExternalApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CurrencyConversionService Unit Tests")
class CurrencyConversionServiceTest {

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private CurrencyConversionService currencyConversionService;

    private RateResponse usdRates;

    @BeforeEach
    void setUp() {
        usdRates = new RateResponse(
                "USD",
                Map.of(
                        "ZAR", new BigDecimal("18.50"),
                        "EUR", new BigDecimal("0.92")
                ),
                LocalDateTime.of(2024, 1, 1, 12, 0)
        );
    }

    @Test
    @DisplayName("convert — returns converted amount and rate used for a valid pair")
    void convert_validPair_returnsConversionResponse() {
        when(exchangeRateService.getRates("USD")).thenReturn(usdRates);

        ConversionResponse result = currencyConversionService.convert("USD", "ZAR", new BigDecimal("100"));

        assertThat(result.fromCurrency()).isEqualTo("USD");
        assertThat(result.toCurrency()).isEqualTo("ZAR");
        assertThat(result.amount()).isEqualByComparingTo("100");
        assertThat(result.exchangeRate()).isEqualByComparingTo("18.50");
        assertThat(result.convertedAmount()).isEqualByComparingTo("1850.0000");
        assertThat(result.rateTimestamp()).isEqualTo(usdRates.fetchedAt());
    }

    @Test
    @DisplayName("convert — rounds the converted amount to 4 decimal places (HALF_UP)")
    void convert_roundsToFourDecimalPlaces() {
        RateResponse rates = new RateResponse(
                "USD",
                Map.of("EUR", new BigDecimal("0.923456789")),
                LocalDateTime.now()
        );
        when(exchangeRateService.getRates("USD")).thenReturn(rates);

        ConversionResponse result = currencyConversionService.convert("USD", "EUR", new BigDecimal("10"));

        assertThat(result.convertedAmount()).isEqualByComparingTo("9.2346");
        assertThat(result.convertedAmount().scale()).isEqualTo(4);
    }

    @Test
    @DisplayName("convert — normalises lowercase currency codes to uppercase")
    void convert_lowercaseInput_normalisedToUppercase() {
        when(exchangeRateService.getRates("USD")).thenReturn(usdRates);

        ConversionResponse result = currencyConversionService.convert("usd", "zar", new BigDecimal("5"));

        assertThat(result.fromCurrency()).isEqualTo("USD");
        assertThat(result.toCurrency()).isEqualTo("ZAR");
        verify(exchangeRateService).getRates("USD");
    }

    @Test
    @DisplayName("convert — throws IllegalArgumentException when amount is null")
    void convert_nullAmount_throwsIllegalArgument() {
        assertThatThrownBy(() -> currencyConversionService.convert("USD", "ZAR", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");

        verifyNoInteractions(exchangeRateService);
    }

    @Test
    @DisplayName("convert — throws IllegalArgumentException when amount is zero")
    void convert_zeroAmount_throwsIllegalArgument() {
        assertThatThrownBy(() -> currencyConversionService.convert("USD", "ZAR", BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(exchangeRateService);
    }

    @Test
    @DisplayName("convert — throws IllegalArgumentException when amount is negative")
    void convert_negativeAmount_throwsIllegalArgument() {
        assertThatThrownBy(() -> currencyConversionService.convert("USD", "ZAR", new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(exchangeRateService);
    }

    @Test
    @DisplayName("convert — throws CurrencyNotFoundException when target rate is unavailable")
    void convert_targetNotInRates_throwsCurrencyNotFound() {
        when(exchangeRateService.getRates("USD")).thenReturn(usdRates);

        assertThatThrownBy(() -> currencyConversionService.convert("USD", "JPY", new BigDecimal("100")))
                .isInstanceOf(CurrencyNotFoundException.class)
                .hasMessageContaining("JPY");
    }

    @Test
    @DisplayName("convert — propagates ExternalApiException raised by the rate service")
    void convert_rateServiceFails_propagatesExternalApiException() {
        when(exchangeRateService.getRates("USD"))
                .thenThrow(new ExternalApiException("upstream down"));

        assertThatThrownBy(() -> currencyConversionService.convert("USD", "ZAR", new BigDecimal("100")))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("upstream down");
    }
}
