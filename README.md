# Currency Exchange Tracker

[![CI](https://github.com/sanimanqezu/currency-exchange-tracker/actions/workflows/ci.yml/badge.svg)](https://github.com/sanimanqezu/currency-exchange-tracker/actions/workflows/ci.yml)

A production-ready Spring Boot 3 REST API for tracking, converting, and analysing currency exchange rates. Built with clean architecture, Caffeine caching, scheduled data ingestion, and a PostgreSQL-backed history store.

**No API key required.** Rates are sourced from the free [open.er-api.com](https://open.er-api.com) API.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 3.5.16 |
| Persistence | Spring Data JPA + PostgreSQL |
| Migrations | Flyway |
| Caching | Spring Cache + Caffeine (1-hour TTL) |
| Scheduling | Spring Scheduler |
| HTTP client | RestTemplate (open.er-api.com) |
| Tests | JUnit 5 + Mockito + AssertJ (H2 in-memory for slice tests) |
| Container | Docker + Docker Compose |
| Utilities | Lombok |

---

## Key design points

- **Layered architecture** — controller → service → client/repository, with records as immutable DTOs and a `@RestControllerAdvice` that maps domain exceptions to RFC 7807 `ProblemDetail` responses.
- **Caching** — rates are cached per base currency with Caffeine (1-hour TTL, 100-entry cap) so repeated reads and conversions avoid redundant upstream calls; `refreshRates` evicts on demand.
- **Resilient external calls** — the `ExchangeRateClient` wraps all upstream failures in a dedicated `ExternalApiException` that surfaces as `503 Service Unavailable`, keeping transport concerns out of the service layer.
- **Scheduled history** — an hourly job snapshots USD/EUR/ZAR rates with upsert semantics (one row per pair per day) and isolates failures per base currency so one bad fetch does not abort the run.
- **Centralized PostgreSQL** — the app joins the shared portfolio Postgres over an external Docker network via `DB_*` environment variables; schema is owned by Flyway and `ddl-auto: validate`.

---

## Getting Started

### Prerequisites
- Java 25
- Maven 3.9+ (use the bundled `./mvnw` wrapper)
- Docker & Docker Compose

### 1. Start the centralized PostgreSQL

This app connects to the shared portfolio PostgreSQL instance over the external `portfolio-net` Docker network. Bring it up first from the portfolio root:

```bash
# in the portfolio-root directory
docker-compose up -d
```

### 2. Run the app

```bash
./mvnw spring-boot:run
```

The API is available at `http://localhost:8080`. Connection details are read from environment variables (defaults in brackets):

| Variable | Default |
|---|---|
| `DB_HOST` | `localhost` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `currency_tracker` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | `postgres` |

Alternatively, build the image and attach it to the shared network with `docker-compose up --build` (the compose file expects the external `portfolio-net` network to already exist).

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

## Tests

```bash
./mvnw test
```

**34 tests**, self-contained — no real database or network access. The external rate API and all collaborators are mocked.

| Layer | Class | What it covers |
|---|---|---|
| Service (unit) | `ExchangeRateServiceTest` | rate fetch/persist, currency validation, case normalisation, compare filtering, upstream failure propagation |
| Service (unit) | `CurrencyConversionServiceTest` | conversion maths, HALF_UP rounding, amount validation, missing-target and upstream-failure cases |
| Service (unit) | `RateSnapshotServiceTest` | history ordering/validation, snapshot upsert (insert + update), per-currency failure isolation |
| Controller (slice) | `ExchangeRateControllerTest` | `@WebMvcTest` status codes, request delegation, 400 on bad/missing params, and `GlobalExceptionHandler` mapping domain exceptions to 404/503 |

Unit tests use Mockito + AssertJ; the controller slice test runs against `MockMvc` with `@MockitoBean` services.

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
