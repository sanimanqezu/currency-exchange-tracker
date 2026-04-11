package com.sanele.currencytracker.repository;

import com.sanele.currencytracker.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    List<ExchangeRate> findByBaseCurrencyOrderByFetchedAtDesc(String baseCurrency);

    Optional<ExchangeRate> findTopByBaseCurrencyAndTargetCurrencyOrderByFetchedAtDesc(
            String baseCurrency, String targetCurrency);

    @Query("SELECT e FROM ExchangeRate e WHERE e.baseCurrency = :base ORDER BY e.fetchedAt DESC")
    List<ExchangeRate> findLatestByBaseCurrency(@Param("base") String baseCurrency);

    void deleteByBaseCurrency(String baseCurrency);
}
