package com.sanele.currencytracker.service;

import com.sanele.currencytracker.dto.HistoryEntry;
import com.sanele.currencytracker.entity.RateSnapshot;
import com.sanele.currencytracker.exception.ExternalApiException;
import com.sanele.currencytracker.repository.RateSnapshotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateSnapshotService Unit Tests")
class RateSnapshotServiceTest {

    @Mock
    private ExchangeRateService exchangeRateService;

    @Mock
    private RateSnapshotRepository rateSnapshotRepository;

    @InjectMocks
    private RateSnapshotService rateSnapshotService;

    @Test
    @DisplayName("getHistory — returns history entries ordered by date ascending")
    void getHistory_returnsEntriesSortedByDate() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 31);

        RateSnapshot later = RateSnapshot.builder()
                .baseCurrency("USD").targetCurrency("ZAR")
                .rate(new BigDecimal("18.70")).snapshotDate(LocalDate.of(2024, 1, 20)).build();
        RateSnapshot earlier = RateSnapshot.builder()
                .baseCurrency("USD").targetCurrency("ZAR")
                .rate(new BigDecimal("18.50")).snapshotDate(LocalDate.of(2024, 1, 5)).build();

        when(rateSnapshotRepository
                .findByBaseCurrencyAndTargetCurrencyAndSnapshotDateBetween("USD", "ZAR", from, to))
                .thenReturn(List.of(later, earlier));

        List<HistoryEntry> result = rateSnapshotService.getHistory("USD", "ZAR", from, to);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(HistoryEntry::date)
                .containsExactly(LocalDate.of(2024, 1, 5), LocalDate.of(2024, 1, 20));
    }

    @Test
    @DisplayName("getHistory — normalises currency codes to uppercase before querying")
    void getHistory_normalisesCurrencyCodes() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 31);
        when(rateSnapshotRepository
                .findByBaseCurrencyAndTargetCurrencyAndSnapshotDateBetween("USD", "ZAR", from, to))
                .thenReturn(List.of());

        rateSnapshotService.getHistory("usd", "zar", from, to);

        verify(rateSnapshotRepository)
                .findByBaseCurrencyAndTargetCurrencyAndSnapshotDateBetween("USD", "ZAR", from, to);
    }

    @Test
    @DisplayName("getHistory — returns an empty list when no snapshots exist")
    void getHistory_noSnapshots_returnsEmptyList() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 31);
        when(rateSnapshotRepository
                .findByBaseCurrencyAndTargetCurrencyAndSnapshotDateBetween("USD", "ZAR", from, to))
                .thenReturn(List.of());

        assertThat(rateSnapshotService.getHistory("USD", "ZAR", from, to)).isEmpty();
    }

    @Test
    @DisplayName("getHistory — throws IllegalArgumentException when 'from' is after 'to'")
    void getHistory_fromAfterTo_throwsIllegalArgument() {
        LocalDate from = LocalDate.of(2024, 2, 1);
        LocalDate to = LocalDate.of(2024, 1, 1);

        assertThatThrownBy(() -> rateSnapshotService.getHistory("USD", "ZAR", from, to))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from");

        verifyNoInteractions(rateSnapshotRepository);
    }

    @Test
    @DisplayName("captureHourlySnapshots — inserts a new snapshot when none exists for the day")
    void captureHourlySnapshots_insertsWhenAbsent() {
        when(exchangeRateService.fetchAndPersistRates(anyString()))
                .thenReturn(java.util.Map.of("ZAR", new BigDecimal("18.50")));
        when(rateSnapshotRepository
                .findByBaseCurrencyAndTargetCurrencyAndSnapshotDate(anyString(), anyString(), any()))
                .thenReturn(Optional.empty());

        rateSnapshotService.captureHourlySnapshots();

        verify(rateSnapshotRepository, atLeastOnce()).save(any(RateSnapshot.class));
        verify(exchangeRateService, times(3)).fetchAndPersistRates(anyString());
    }

    @Test
    @DisplayName("captureHourlySnapshots — updates the existing snapshot (upsert) when present")
    void captureHourlySnapshots_updatesWhenPresent() {
        RateSnapshot existing = RateSnapshot.builder()
                .baseCurrency("USD").targetCurrency("ZAR")
                .rate(new BigDecimal("18.00")).snapshotDate(LocalDate.now()).build();

        when(exchangeRateService.fetchAndPersistRates(anyString()))
                .thenReturn(java.util.Map.of("ZAR", new BigDecimal("18.50")));
        when(rateSnapshotRepository
                .findByBaseCurrencyAndTargetCurrencyAndSnapshotDate(anyString(), anyString(), any()))
                .thenReturn(Optional.of(existing));

        rateSnapshotService.captureHourlySnapshots();

        assertThat(existing.getRate()).isEqualByComparingTo("18.50");
        verify(rateSnapshotRepository, atLeastOnce()).save(existing);
    }

    @Test
    @DisplayName("captureHourlySnapshots — keeps processing other currencies when one fails")
    void captureHourlySnapshots_oneCurrencyFails_othersStillProcessed() {
        when(exchangeRateService.fetchAndPersistRates("USD"))
                .thenThrow(new ExternalApiException("USD feed down"));
        when(exchangeRateService.fetchAndPersistRates("EUR"))
                .thenReturn(java.util.Map.of("ZAR", new BigDecimal("20.1")));
        when(exchangeRateService.fetchAndPersistRates("ZAR"))
                .thenReturn(java.util.Map.of("USD", new BigDecimal("0.05")));
        when(rateSnapshotRepository
                .findByBaseCurrencyAndTargetCurrencyAndSnapshotDate(anyString(), anyString(), any()))
                .thenReturn(Optional.empty());

        assertThatCode(() -> rateSnapshotService.captureHourlySnapshots()).doesNotThrowAnyException();

        verify(exchangeRateService, times(3)).fetchAndPersistRates(anyString());
        verify(rateSnapshotRepository, atLeast(2)).save(any(RateSnapshot.class));
    }
}
