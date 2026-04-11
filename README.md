# Currency Exchange Tracker

A production-ready Spring Boot 3 REST API for tracking, converting, and analysing currency exchange rates. Built with clean architecture, Caffeine caching, scheduled data ingestion, and a PostgreSQL-backed history store.

**No API key required.** Rates are sourced from the free [open.er-api.com](https://open.er-api.com) API.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Persistence | Spring Data JPA + PostgreSQL |
| Migrations | Flyway |
| Caching | Spring Cache + Caffeine (1-hour TTL) |
| Scheduling | Spring Scheduler |
| Tests | JUnit 5 + Mockito (H2 in-memory) |
| Container | Docker + Docker Compose |
| Utilities | Lombok |

---

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.9+
- Docker & Docker Compose (for containerised run)

### Run with Docker Compose (recommended)

```bash
docker-compose up --build
```

This starts PostgreSQL and the Spring Boot app. The API is available at `http://localhost:8080`.

### Run locally (requires PostgreSQL running)

```bash
# Start PostgreSQL separately, then:
./mvnw spring-boot:run
```

Default datasource: `jdbc:postgresql://localhost:5432/currency_tracker` with username/password `postgres/postgres`.  
Override via environment variables: `DB_USERNAME`, `DB_PASSWORD`.

---

## API Endpoints

### GET /api/rates/{base}
Fetch current exchange rates for a base currency. Results cached for 1 hour.

```
GET /api/rates/USD
```

**Response:**
```json
{
  "baseCurrency": "USD",
  "rates": {
    "EUR": 0.92,
    "ZAR": 18.50,
    "GBP": 0.79
  },
  "fetchedAt": "2024-03-01T12:00:00"
}
```

---

### GET /api/rates/convert
Convert an amount between two currencies.

```
GET /api/rates/convert?from=USD&to=ZAR&amount=100
```

**Response:**
```json
{
  "fromCurrency": "USD",
  "toCurrency": "ZAR",
  "amount": 100,
  "convertedAmount": 1850.0000,
  "exchangeRate": 18.50,
  "rateTimestamp": "2024-03-01T12:00:00"
}
```

---

### GET /api/rates/history/{base}/{target}
Retrieve historical daily snapshot rates between two dates.

```
GET /api/rates/history/USD/ZAR?from=2024-01-01&to=2024-03-01
```

**Response:**
```json
[
  { "baseCurrency": "USD", "targetCurrency": "ZAR", "rate": 18.72, "date": "2024-01-01" },
  { "baseCurrency": "USD", "targetCurrency": "ZAR", "rate": 18.65, "date": "2024-01-02" }
]
```

---

### GET /api/rates/supported
Returns the list of ~20 supported currency codes.

```
GET /api/rates/supported
```

**Response:**
```json
["USD", "EUR", "GBP", "ZAR", "JPY", "CAD", "AUD", "CHF", "CNY", "INR",
 "MXN", "BRL", "SGD", "HKD", "NOK", "SEK", "DKK", "NZD", "KRW", "TRY"]
```

---

### GET /api/rates/compare
Compare multiple exchange rates against a base currency in one call.

```
GET /api/rates/compare?base=USD&targets=EUR,GBP,ZAR,JPY
```

**Response:**
```json
{
  "baseCurrency": "USD",
  "rates": {
    "EUR": 0.92,
    "GBP": 0.79,
    "ZAR": 18.50,
    "JPY": 149.50
  },
  "fetchedAt": "2024-03-01T12:00:00"
}
```

---

## Caching

Exchange rates are cached per base currency using **Caffeine** with a **1-hour TTL**. Cache size is capped at 100 entries. Cache statistics are enabled and logged at DEBUG level.

---

## Scheduled Snapshots

Every hour (`0 0 * * * *`) the scheduler automatically fetches rates for **USD**, **EUR**, and **ZAR** and stores daily snapshots in the `rate_snapshots` table. This builds up historical data over time which is queryable via the `/history` endpoint.

---

## Database Schema

```sql
-- Live rate records (appended each fetch)
exchange_rates (id, base_currency, target_currency, rate, fetched_at)

-- Daily snapshots (one row per pair per day, upserted hourly)
rate_snapshots (id, base_currency, target_currency, rate, snapshot_date)
```

Migrations are managed by **Flyway** and run automatically on startup.

---

## Running Tests

```bash
./mvnw test
```

Tests use an **H2 in-memory database** — no PostgreSQL required. Flyway is disabled in the test profile.

---

## Project Structure

```
src/
├── main/java/com/sanele/currencytracker/
│   ├── CurrencyTrackerApplication.java
│   ├── client/
│   │   ├── ExchangeRateClient.java       # HTTP client for open.er-api.com
│   │   └── ExchangeRateApiResponse.java  # API response record
│   ├── config/
│   │   ├── AppConfig.java                # RestTemplate bean
│   │   └── CacheConfig.java              # Caffeine cache manager
│   ├── controller/
│   │   └── ExchangeRateController.java
│   ├── dto/                              # Response records
│   ├── entity/                           # JPA entities
│   ├── exception/                        # Custom exceptions + GlobalExceptionHandler
│   ├── repository/                       # Spring Data JPA repositories
│   └── service/
│       ├── ExchangeRateService.java      # Core service with @Cacheable
│       ├── CurrencyConversionService.java
│       └── RateSnapshotService.java      # @Scheduled snapshot job
└── main/resources/
    ├── application.yml
    └── db/migration/V1__create_tables.sql
```

---

## External API

This project uses the **[Open Exchange Rates API](https://open.er-api.com)** free tier:

- **Endpoint:** `https://open.er-api.com/v6/latest/{base}`
- **Authentication:** None required — completely free, no API key
- **Rate limit:** 1,500 requests/month on the free plan
- **Data freshness:** Updated every ~60 minutes

---

## License

MIT
