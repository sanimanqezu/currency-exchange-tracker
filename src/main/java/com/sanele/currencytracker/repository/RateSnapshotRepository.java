package com.sanele.currencytracker.repository;

import com.sanele.currencytracker.entity.RateSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RateSnapshotRepository extends JpaRepository<RateSnapshot, Long> {

    List<RateSnapshot> findByBaseCurrencyAndTargetCurrencyAndSnapshotDateBetween(
            String baseCurrency,
            String targetCurrency,
            LocalDate from,
            LocalDate to
    );

    Optional<RateSnapshot> findByBaseCurrencyAndTargetCurrencyAndSnapshotDate(
            String baseCurrency,
            String targetCurrency,
            LocalDate snapshotDate
    );

    List<RateSnapshot> findByBaseCurrencyOrderBySnapshotDateDesc(String baseCurrency);
}
