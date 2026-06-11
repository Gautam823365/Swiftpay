🚀 SwiftPay — High-Performance Distributed Payment System

A production-style, event-driven payment processing system built with Spring Boot, Kafka, PostgreSQL, and Redis, designed to simulate real-world fintech ledger workloads and validated under high-throughput load testing (250 TPS, 1M+ transactions) using k6.

📌 Overview

SwiftPay simulates a real-world payment infrastructure used in fintech systems where:

Payments are initiated via a Gateway Service
Events are published to Kafka
A Ledger Service processes transactions atomically
Account balances are updated safely using database row-level locking
Transaction results are published back to Kafka

🏗️ System Architecture
Client
   ↓
Gateway Service
   ↓
Kafka (payment.initiated)
   ↓
Ledger Service
   ↓
PostgreSQL (Accounts + Transactions)
   ↓
Kafka (payment.completed / payment.failed)
⚙️ Tech Stack
☕ Java 21 + Spring Boot
📨 Apache Kafka (event-driven architecture)
🐘 PostgreSQL (ACID transactions)
🔴 Redis (idempotency + deduplication)
📊 k6 (load testing)
📈 Grafana (observability & metrics)
🐳 Docker Compose (local infrastructure)
🔄 Payment Flow
Client sends request to Gateway (POST /v1/payments)
Gateway Service:
Validates request
Ensures idempotency using Redis
Persists transaction as PENDING
Publishes PaymentInitiatedEvent to Kafka
Ledger Service:
Consumes event from Kafka
Locks sender & receiver accounts (deadlock-safe ordering)
Performs atomic debit/credit
Updates transaction status
Result is published to Kafka:
payment.completed
payment.failed
🚀 API
Create Payment

POST /v1/payments

{
  "idempotencyKey": "a12b34c56d78e90f",
  "senderId": "uuid",
  "receiverId": "uuid",
  "amount": 100.50,
  "currency": "USD"
}
Response
{
  "transactionId": "uuid",
  "status": "PENDING"
}
🧪 Load Testing (k6)
Scenario
Constant load: 250 TPS
Duration: 15 minutes
Total requests: ~225K–300K per run
Max VUs: 1000
Run Command
k6 run -e BASE_URL=http://localhost:8080 load-test.js
📊 Performance Results
Observed Metrics
✅ Success rate: 99.99%+
⚡ Average latency: 18ms – 150ms
📈 P95 latency: < 100ms (steady state)
❗ Minimal failure rate under sustained load
System Behavior Under Load
Kafka maintained stable throughput with no backlog spikes
HikariCP connection pool remained stable under tuning
No service crashes under sustained 250 TPS workload
🧠 Key Engineering Decisions
1. Event-Driven Architecture
Decouples Gateway and Ledger services
Enables horizontal scalability
Improves resilience under load
2. Redis-based Idempotency
Prevents duplicate payments
Ensures safe retry behavior under load
3. Deadlock-Safe Account Locking
Accounts locked in deterministic UUID order
Prevents circular wait conditions in DB
4. Kafka Async Processing
High throughput, non-blocking pipeline
Supports horizontal scaling via partitions
⚡ Performance Tuning
Database (HikariCP)
maximum-pool-size: 40
minimum-idle: 5
connection-timeout: 30000
Kafka
Async producer enabled
JSON serialization for event payloads
Partition-based scaling enabled
Service Optimization
Lightweight DTO-based event payloads
Reduced blocking logic in API layer
Optimized transaction boundaries
📦 Running Locally
1. Start infrastructure
docker-compose up -d
2. Build services
mvn clean install -DskipTests
3. Run services
# Gateway Service
cd gateway-service && mvn spring-boot:run

# Ledger Service
cd ledger-service && mvn spring-boot:run
📈 Monitoring

Grafana dashboards track:

TPS (Throughput)
Kafka consumer lag
Database connection pool usage
API latency (p95/p99)
Error rates
📡 Load Test Artifacts
k6 script: /load-test/load-test.js
Results logs: /load-test/results/
Optional: PCAP capture via tcpdump / Wireshark
🧩 Future Improvements
Add OpenTelemetry distributed tracing
Kafka retry + DLQ mechanism
Saga pattern for multi-step payments
Rate limiting at API gateway
Ledger sharding by region/account segments
🏁 Summary

SwiftPay demonstrates a production-grade distributed payment system capable of:

Sustaining 250 TPS+ load
Processing hundreds of thousands of transactions reliably
Maintaining low-latency (<100ms p95) performance
Ensuring safe, ACID-compliant financial operations
