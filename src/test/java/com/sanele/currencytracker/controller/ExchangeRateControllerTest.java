package com.sanele.currencytracker.controller;

import com.sanele.currencytracker.dto.CompareResponse;
import com.sanele.currencytracker.dto.ConversionResponse;
import com.sanele.currencytracker.dto.HistoryEntry;
import com.sanele.currencytracker.dto.RateResponse;
import com.sanele.currencytracker.exception.CurrencyNotFoundException;
import com.sanele.currencytracker.exception.ExternalApiException;
import com.sanele.currencytracker.service.CurrencyConversionService;
import com.sanele.currencytracker.service.ExchangeRateService;
import com.sanele.currencytracker.service.RateSnapshotService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExchangeRateController.class)
@DisplayName("ExchangeRateController Slice Tests")
class ExchangeRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExchangeRateService exchangeRateService;

    @MockitoBean
    private CurrencyConversionService currencyConversionService;

    @MockitoBean
    private RateSnapshotService rateSnapshotService;

    @Test
    @DisplayName("GET /api/rates/{base} — returns 200 and delegates to the service")
    void getRates_returnsOk() throws Exception {
        RateResponse response = new RateResponse("USD",
                Map.of("ZAR", new BigDecimal("18.50")), LocalDateTime.of(2024, 1, 1, 0, 0));
        when(exchangeRateService.getRates("USD")).thenReturn(response);

        mockMvc.perform(get("/api/rates/USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseCurrency").value("USD"))
                .andExpect(jsonPath("$.rates.ZAR").value(18.50));

        verify(exchangeRateService).getRates("USD");
    }

    @Test
    @DisplayName("GET /api/rates/{base} — maps CurrencyNotFoundException to 404 problem detail")
    void getRates_unsupportedCurrency_returns404() throws Exception {
        when(exchangeRateService.getRates("XYZ"))
                .thenThrow(new CurrencyNotFoundException("Currency 'XYZ' is not supported"));

        mockMvc.perform(get("/api/rates/XYZ"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Currency Not Found"))
                .andExpect(jsonPath("$.detail").value("Currency 'XYZ' is not supported"));
    }

    @Test
    @DisplayName("GET /api/rates/{base} — maps ExternalApiException to 503 problem detail")
    void getRates_externalApiFails_returns503() throws Exception {
        when(exchangeRateService.getRates("USD"))
                .thenThrow(new ExternalApiException("upstream down"));

        mockMvc.perform(get("/api/rates/USD"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("External API Unavailable"));
    }

    @Test
    @DisplayName("GET /api/rates/convert — returns 200 with the conversion result")
    void convert_returnsOk() throws Exception {
        ConversionResponse response = new ConversionResponse(
                "USD", "ZAR", new BigDecimal("100"), new BigDecimal("1850.0000"),
                new BigDecimal("18.50"), LocalDateTime.of(2024, 1, 1, 0, 0));
        when(currencyConversionService.convert("USD", "ZAR", new BigDecimal("100"))).thenReturn(response);

        mockMvc.perform(get("/api/rates/convert").param("from", "USD").param("to", "ZAR").param("amount", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.convertedAmount").value(1850.0000))
                .andExpect(jsonPath("$.exchangeRate").value(18.50));
    }

    @Test
    @DisplayName("GET /api/rates/convert — missing required parameter returns 400")
    void convert_missingParam_returns400() throws Exception {
        mockMvc.perform(get("/api/rates/convert").param("from", "USD").param("to", "ZAR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Missing Required Parameter"));

        verifyNoInteractions(currencyConversionService);
    }

    @Test
    @DisplayName("GET /api/rates/convert — non-numeric amount returns 400")
    void convert_invalidAmountType_returns400() throws Exception {
        mockMvc.perform(get("/api/rates/convert")
                        .param("from", "USD").param("to", "ZAR").param("amount", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Parameter Type"));

        verifyNoInteractions(currencyConversionService);
    }

    @Test
    @DisplayName("GET /api/rates/convert — IllegalArgumentException from service maps to 400")
    void convert_invalidAmountValue_returns400() throws Exception {
        when(currencyConversionService.convert(eq("USD"), eq("ZAR"), any()))
                .thenThrow(new IllegalArgumentException("Amount must be a positive number"));

        mockMvc.perform(get("/api/rates/convert")
                        .param("from", "USD").param("to", "ZAR").param("amount", "-5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Request"));
    }

    @Test
    @DisplayName("GET /api/rates/supported — returns 200 with supported currency list")
    void getSupported_returnsOk() throws Exception {
        when(exchangeRateService.getSupportedCurrencies()).thenReturn(List.of("USD", "EUR", "ZAR"));

        mockMvc.perform(get("/api/rates/supported"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("USD"))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("GET /api/rates/compare — returns 200 and parses the targets list")
    void compare_returnsOk() throws Exception {
        CompareResponse response = new CompareResponse("USD",
                Map.of("EUR", new BigDecimal("0.92"), "GBP", new BigDecimal("0.79")),
                LocalDateTime.of(2024, 1, 1, 0, 0));
        when(exchangeRateService.compareRates(eq("USD"), eq(List.of("EUR", "GBP")))).thenReturn(response);

        mockMvc.perform(get("/api/rates/compare").param("base", "USD").param("targets", "EUR,GBP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseCurrency").value("USD"))
                .andExpect(jsonPath("$.rates.EUR").value(0.92));

        verify(exchangeRateService).compareRates("USD", List.of("EUR", "GBP"));
    }

    @Test
    @DisplayName("GET /api/rates/compare — blank targets list returns 400")
    void compare_emptyTargets_returns400() throws Exception {
        mockMvc.perform(get("/api/rates/compare").param("base", "USD").param("targets", " , "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Request"));

        verifyNoInteractions(exchangeRateService);
    }

    @Test
    @DisplayName("GET /api/rates/history/{base}/{target} — returns 200 with history entries")
    void getHistory_returnsOk() throws Exception {
        HistoryEntry entry = new HistoryEntry("USD", "ZAR",
                new BigDecimal("18.50"), LocalDate.of(2024, 1, 5));
        when(rateSnapshotService.getHistory(eq("USD"), eq("ZAR"), any(), any()))
                .thenReturn(List.of(entry));

        mockMvc.perform(get("/api/rates/history/USD/ZAR")
                        .param("from", "2024-01-01").param("to", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].targetCurrency").value("ZAR"))
                .andExpect(jsonPath("$[0].rate").value(18.50));
    }
}
