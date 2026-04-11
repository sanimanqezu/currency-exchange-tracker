package com.sanele.currencytracker.service;

import com.sanele.currencytracker.dto.HistoryEntry;
import com.sanele.currencytracker.entity.RateSnapshot;
import com.sanele.currencytracker.repository.RateSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateSnapshotService {

    /**
     * Currencies for which daily snapshots are automatically stored by the scheduler.
     */
    private static final List<String> SCHEDULED_BASE_CURRENCIES = List.of("USD", "EUR", "ZAR");

    private final ExchangeRateService exchangeRateService;
    private final RateSnapshotRepository rateSnapshotRepository;

    /**
     * Scheduled job: every hour, fetch rates for USD, EUR, ZAR and save a daily snapshot.
     * If a snapshot for today already exists for a given pair, it is updated (upsert behaviour).
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void captureHourlySnapshots() {
        log.info("Scheduled snapshot job started at {}", LocalDateTime.now());
        for (String base : SCHEDULED_BASE_CURRENCIES) {
            try {
                Map<String, BigDecimal> rates = exchangeRateService.fetchAndPersistRates(base);
                saveSnapshots(base, rates, LocalDate.now());
                log.info("Snapshot saved for base currency {}", base);
            } catch (Exception e) {
                log.error("Failed to save snapshot for base {}: {}", base, e.getMessage(), e);
            }
        }
        log.info("Scheduled snapshot job completed at {}", LocalDateTime.now());
    }

    /**
     * Returns historical rate data for a currency pair within the specified date range.
     *
     * @param baseCurrency   the base currency code
     * @param targetCurrency the target currency code
     * @param from           start date (inclusive)
     * @param to             end date (inclusive)
     * @return list of HistoryEntry records ordered by date
     */
    @Transactional(readOnly = true)
    public List<HistoryEntry> getHistory(String baseCurrency, String targetCurrency, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' date must not be after 'to' date");
        }

        List<RateSnapshot> snapshots = rateSnapshotRepository
                .findByBaseCurrencyAndTargetCurrencyAndSnapshotDateBetween(
                        baseCurrency.toUpperCase(),
                        targetCurrency.toUpperCase(),
                        from,
                        to
                );

        return snapshots.stream()
                .map(s -> new HistoryEntry(s.getBaseCurrency(), s.getTargetCurrency(), s.getRate(), s.getSnapshotDate()))
                .sorted((a, b) -> a.date().compareTo(b.date()))
                .collect(Collectors.toList());
    }

    private void saveSnapshots(String base, Map<String, BigDecimal> rates, LocalDate snapshotDate) {
        for (Map.Entry<String, BigDecimal> entry : rates.entrySet()) {
            String target = entry.getKey();
            BigDecimal rate = entry.getValue();

            // Upsert: update if exists, insert if not
            rateSnapshotRepository
                    .findByBaseCurrencyAndTargetCurrencyAndSnapshotDate(base, target, snapshotDate)
                    .ifPresentOrElse(
                            existing -> {
                                existing.setRate(rate);
                                rateSnapshotRepository.save(existing);
                            },
                            () -> rateSnapshotRepository.save(
                                    RateSnapshot.builder()
                                            .baseCurrency(base)
                                            .targetCurrency(target)
                                            .rate(rate)
                                            .snapshotDate(snapshotDate)
                                            .build()
                            )
                    );
        }
    }
}
