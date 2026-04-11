package com.sanele.currencytracker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "rate_snapshots",
        indexes = {
                @Index(name = "idx_snapshot_base", columnList = "base_currency"),
                @Index(name = "idx_snapshot_pair_date", columnList = "base_currency, target_currency, snapshot_date")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_snapshot_pair_date",
                        columnNames = {"base_currency", "target_currency", "snapshot_date"}
                )
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "base_currency", nullable = false, length = 10)
    private String baseCurrency;

    @Column(name = "target_currency", nullable = false, length = 10)
    private String targetCurrency;

    @Column(name = "rate", nullable = false, precision = 20, scale = 8)
    private BigDecimal rate;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;
}
