package com.sanele.currencytracker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_rates",
        indexes = {
                @Index(name = "idx_exchange_rate_base", columnList = "base_currency"),
                @Index(name = "idx_exchange_rate_pair", columnList = "base_currency, target_currency")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "base_currency", nullable = false, length = 10)
    private String baseCurrency;

    @Column(name = "target_currency", nullable = false, length = 10)
    private String targetCurrency;

    @Column(name = "rate", nullable = false, precision = 20, scale = 8)
    private BigDecimal rate;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;
}
