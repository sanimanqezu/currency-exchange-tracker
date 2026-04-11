package com.sanele.currencytracker.service;

import com.sanele.currencytracker.client.ExchangeRateClient;
import com.sanele.currencytracker.dto.CompareResponse;
import com.sanele.currencytracker.dto.RateResponse;
import com.sanele.currencytracker.entity.ExchangeRate;
import com.sanele.currencytracker.exception.CurrencyNotFoundException;
import com.sanele.currencytracker.exception.ExternalApiException;
import com.sanele.currencytracker.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateService Unit Tests")
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateClient exchangeRateClient;

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    private Map<String, BigDecimal> mockRates;

    @BeforeEach
    void setUp() {
        mockRates = Map.of(
                "EUR", new BigDecimal("0.92"),
                "GBP", new BigDecimal("0.79"),
                "ZAR", new BigDecimal("18.50"),
                "JPY", new BigDecimal("149.50"),
                "CAD", new BigDecimal("1.36")
        );
    }

    // -------------------------------------------------------------------------
    // Test 1: getRates returns correct RateResponse for valid currency
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("getRates — returns RateResponse with rates for a valid base currency")
    void getRates_validCurrency_returnsRateResponse() {
        when(exchangeRateClient.fetchRates("USD")).thenReturn(mockRates);
        when(exchangeRateRepository.saveAll(anyList())).thenReturn(List.of());

        RateResponse result = exchangeRateService.getRates("USD");

        assertThat(result).isNotNull();
        assertThat(result.baseCurrency()).isEqualTo("USD");
        assertThat(result.rates()).containsKey("EUR");
        assertThat(result.rates().get("ZAR")).isEqualByComparingTo(new BigDecimal("18.50"));
        assertThat(result.fetchedAt()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // Test 2: getRates normalises currency code to uppercase
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("getRates — normalises lowercase currency input to uppercase")
    void getRates_lowercaseInput_normalisedToUppercase() {
        when(exchangeRateClient.fetchRates("USD")).thenReturn(mockRates);
        when(exchangeRateRepository.saveAll(anyList())).thenReturn(List.of());

        RateResponse result = exchangeRateService.getRates("usd");

        assertThat(result.baseCurrency()).isEqualTo("USD");
        verify(exchangeRateClient).fetchRates("USD");
    }

    // -------------------------------------------------------------------------
    // Test 3: getRates throws CurrencyNotFoundException for unsupported currency
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("getRates — throws CurrencyNotFoundException for an unsupported currency code")
    void getRates_unsupportedCurrency_throwsCurrencyNotFoundException() {
        assertThatThrownBy(() -> exchangeRateService.getRates("XYZ"))
                .isInstanceOf(CurrencyNotFoundException.class)
                .hasMessageContaining("XYZ");

        verifyNoInteractions(exchangeRateClient);
        verifyNoInteractions(exchangeRateRepository);
    }

    // -------------------------------------------------------------------------
    // Test 4: getRates persists all returned rates to the database
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("getRates — persists all fetched rates to the database")
    void getRates_validCurrency_persistsRatesToDatabase() {
        when(exchangeRateClient.fetchRates("EUR")).thenReturn(mockRates);
        when(exchangeRateRepository.saveAll(anyList())).thenReturn(List.of());

        exchangeRateService.getRates("EUR");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ExchangeRate>> captor = ArgumentCaptor.forClass(List.class);
        verify(exchangeRateRepository).saveAll(captor.capture());

        List<ExchangeRate> saved = captor.getValue();
        assertThat(saved).hasSize(mockRates.size());
        assertThat(saved).allMatch(e -> "EUR".equals(e.getBaseCurrency()));
        assertThat(saved).allMatch(e -> e.getFetchedAt() != null);
    }

    // -------------------------------------------------------------------------
    // Test 5: getRates propagates ExternalApiException from client
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("getRates — propagates ExternalApiException when client fails")
    void getRates_clientThrowsException_propagatesExternalApiException() {
        when(exchangeRateClient.fetchRates("USD"))
                .thenThrow(new ExternalApiException("API timeout"));

        assertThatThrownBy(() -> exchangeRateService.getRates("USD"))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("API timeout");
    }

    // -------------------------------------------------------------------------
    // Test 6: getSupportedCurrencies returns non-empty list with known currencies
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("getSupportedCurrencies — returns list containing USD, EUR, ZAR")
    void getSupportedCurrencies_returnsExpectedCurrencies() {
        List<String> supported = exchangeRateService.getSupportedCurrencies();

        assertThat(supported).isNotEmpty();
        assertThat(supported).contains("USD", "EUR", "ZAR", "GBP", "JPY");
    }

    // -------------------------------------------------------------------------
    // Test 7: compareRates filters to only the requested target currencies
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("compareRates — filters rates to only requested target currencies")
    void compareRates_filtersToRequestedTargets() {
        when(exchangeRateClient.fetchRates("USD")).thenReturn(mockRates);
        when(exchangeRateRepository.saveAll(anyList())).thenReturn(List.of());

        CompareResponse response = exchangeRateService.compareRates("USD", List.of("EUR", "GBP"));

        assertThat(response.baseCurrency()).isEqualTo("USD");
        assertThat(response.rates()).containsOnlyKeys("EUR", "GBP");
        assertThat(response.rates().get("EUR")).isEqualByComparingTo(new BigDecimal("0.92"));
    }

    // -------------------------------------------------------------------------
    // Test 8: compareRates silently skips unknown target currencies
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("compareRates — skips unknown target currencies without throwing")
    void compareRates_unknownTargetCurrency_skipsGracefully() {
        when(exchangeRateClient.fetchRates("USD")).thenReturn(mockRates);
        when(exchangeRateRepository.saveAll(anyList())).thenReturn(List.of());

        CompareResponse response = exchangeRateService.compareRates("USD", List.of("EUR", "INVALID", "GBP"));

        assertThat(response.rates()).containsOnlyKeys("EUR", "GBP");
        assertThat(response.rates()).doesNotContainKey("INVALID");
    }
}
