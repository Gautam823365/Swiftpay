# SwiftPay — Real-Time Payment Ledger

A resilient, event-driven P2P payment platform built with Spring Boot 3, Kafka, Redis, and PostgreSQL.

## Architecture

```
Client → Gateway Service (8080) → Kafka → Ledger Service (8081) → PostgreSQL
                     ↑↓ Redis (idempotency + balance cache)
```

## Quick Start

```bash
# Start the full stack
docker compose up --build

# Gateway Swagger UI
open http://localhost:8080/swagger-ui.html

# Ledger Swagger UI
open http://localhost:8081/swagger-ui.html
```

## API

### POST /v1/payments
Initiate a P2P payment. Idempotent via `idempotencyKey`.

```json
{
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000",
  "senderId":       "a0000000-0000-0000-0000-000000000001",
  "receiverId":     "a0000000-0000-0000-0000-000000000002",
  "amount":         "100.00",
  "currency":       "USD"
}
```

**Responses:**
- `202 Accepted` — payment queued
- `400 Bad Request` — validation failure
- `404 Not Found` — unknown account
- `409 Conflict` — duplicate idempotency key
- `422 Unprocessable Entity` — insufficient funds

### GET /v1/ledger/{userId}/transactions?page=0&size=20
Paginated transaction history for a user.

## Seed Accounts (for testing)

| ID | Balance |
|----|---------|
| `a0000000-0000-0000-0000-000000000001` | $10,000 |
| `a0000000-0000-0000-0000-000000000002` | $5,000  |
| `a0000000-0000-0000-0000-000000000003` | $250    |

## Load Test

```bash
# Requires k6 (https://k6.io/docs/get-started/installation/)
k6 run load-test/k6-script.js

# Custom base URL
k6 run -e BASE_URL=http://localhost:8080 load-test/k6-script.js
```

Target: 250 TPS sustained for 60 minutes (~1M transactions).

Capture PCAP while running:
```bash
sudo tcpdump -i lo -w trace.pcap port 8080 or port 5432 or port 9092
```

## Running Tests

```bash
# Unit tests (no infra needed)
cd gateway-service && mvn test
cd ledger-service  && mvn test

# Integration tests (Testcontainers spins up Postgres + Kafka + Redis automatically)
cd gateway-service && mvn verify
cd ledger-service  && mvn verify
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `swiftpay` | Database name |
| `DB_USER` | `swiftpay` | DB username |
| `DB_PASS` | `swiftpay` | DB password |
| `KAFKA_BROKERS` | `localhost:9092` | Kafka bootstrap servers |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |

## Health Checks

- Gateway: `GET http://localhost:8080/actuator/health`
- Ledger:  `GET http://localhost:8081/actuator/health`
#   S w i f t p a y 

# Pcap file
 https://drive.google.com/drive/folders/1aeNJaf-BvFdCWjc0UWEmjcAf0vuatvQG?usp=sharing

# k6 Script image
<img width="1292" height="703" alt="Screenshot 2026-06-12 162040" src="https://github.com/user-attachments/assets/172d76e9-619d-409b-9487-4bd20e4206b9" />

 
