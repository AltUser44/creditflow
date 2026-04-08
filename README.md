# CreditFlow

Event-driven microservices demo: **user authentication**, **transactions**, and a **credit score** service that reacts to transaction events published on **Kafka** and stores scores in **PostgreSQL**.

## Architecture

```
                    +------------------+
                    |   PostgreSQL     |
                    | users / tx /     |
                    | credit DBs       |
                    +--------+---------+
                             ^
           +-----------------+------------------+
           |                 |                  |
    +------+------+   +------+------+    +------+------+
    | user-service|   |transaction |    |credit-service|
    |   :8081     |   |  service   |    |   :8083      |
    | JWT, users  |   |  :8082     |    | consumer +   |
    +------+------+   | persist +  |    | GET score    |
           |          |  produce   |    +------+------+
           |          +------+-----+           ^
           |                 |                 |
           |                 v                 |
           |          +------+------+          |
           |          |   Kafka     |---------+
           |          | transaction-|
           |          | events topic|
           |          +-------------+
           |
      curl / clients
```

**Flow:** `transaction-service` saves a row, then publishes a JSON event to topic `transaction-events`. `credit-service` consumes each message, records it for rolling-window rules, updates the user’s score in `credit_db`, and exposes the latest score over HTTP.

## Why Kafka?

- **Decoupling:** The transaction API does not need to know how scores are computed; new consumers can be added without changing the producer.
- **Scalability:** Multiple `credit-service` instances can share a **consumer group** to parallelize partitions as load grows.
- **Durability & replay:** Events are retained on the log so you can reprocess history when scoring rules change (with operational caveats around idempotency).

## Tech stack

| Layer        | Choice |
|-------------|--------|
| Language    | Scala 2.13 |
| HTTP        | Akka HTTP |
| JSON        | Circe, akka-http-circe |
| DB access   | Doobie (PostgreSQL) |
| Auth        | BCrypt, JWT (HS256) |
| Messaging   | Apache Kafka (Zookeeper mode in Docker) |
| Packaging   | sbt-native-packager (`stage`) |

## Scoring rules (credit-service)

- Default score **600** if the user has no row in `credit_scores`.
- **Debit** with amount **> 1000** (absolute value): **-10**.
- **Credit**: **+5**.
- **Frequent activity:** if the user has **≥ 3** events in `score_events` in the **last 24 hours** (including the current event): **+2** on that update.
- Score is clamped to **[0, 1000]** after each update.

## Prerequisites

- **Docker** and **Docker Compose** v2 (for `service_completed_successfully` / health conditions; use a recent Compose).
- For **local** runs without Docker: JDK 17+, [sbt](https://www.scala-sbt.org/), PostgreSQL, and Kafka — or run only infra via Compose and services via `sbt`.

## Local development (no Docker)

Install **PostgreSQL**, **Apache Kafka** (with **ZooKeeper**), **JDK 17**, and **sbt**. Add `psql` and Kafka `bin` (Windows: `bin\windows`) to your `PATH` where useful.

### 1. Start ZooKeeper and Kafka

Use your Kafka distribution’s scripts (example names):

- ZooKeeper: `zookeeper-server-start` (or `bin\windows\zookeeper-server-start.bat` on Windows)
- Broker: `kafka-server-start` with a `server.properties` that listens on **`localhost:9092`**

### 2. Create the Kafka topic

From the `creditflow` directory:

```bash
./scripts/local/create-kafka-topic.sh
```

Windows PowerShell:

```powershell
.\scripts\local\create-kafka-topic.ps1
```

Or manually:

```bash
kafka-topics --bootstrap-server localhost:9092 --create --if-not-exists --topic transaction-events --partitions 1 --replication-factor 1
```

### 3. Provision PostgreSQL

Default app user is **`creditflow`** / **`creditflow`** (matches `application.conf`). Set `PGPASSWORD` to your **PostgreSQL superuser** password (often `postgres`), then run:

**Linux / macOS / Git Bash:**

```bash
export PGPASSWORD='<superuser password>'
./scripts/local/setup-postgres.sh
```

**Windows PowerShell** (from repo root):

```powershell
$env:PGPASSWORD = "<superuser password>"
.\scripts\local\setup-postgres.ps1
```

This creates the role `creditflow` (if missing), databases `users_db`, `transactions_db`, `credit_db` (if missing), and `CREATE TABLE` scripts under [`scripts/local/sql/`](scripts/local/sql/).

### 4. Build and run the services

```bash
cd creditflow
sbt compile
```

Three terminals:

```bash
sbt "userService/run"
sbt "transactionService/run"
sbt "creditService/run"
```

Defaults expect **PostgreSQL** on `localhost:5432` and **Kafka** on `localhost:9092`. Override with:

- `JDBC_URL`, `DB_USER`, `DB_PASSWORD` per service
- `KAFKA_BOOTSTRAP_SERVERS` for transaction and credit services
- `JWT_SECRET` for user-service

### 5. Smoke test

```bash
./scripts/test-flow.sh
```

or `.\scripts\test-flow.ps1` on Windows.

---

## Quick start (Docker)

From the `creditflow` directory:

```bash
docker compose up --build
```

Wait until `postgres` is healthy, `kafka-init` exits successfully, and all three app services are listening.

- User API: `http://localhost:8081`
- Transaction API: `http://localhost:8082`
- Credit API: `http://localhost:8083`
- Kafka (host): `localhost:9092` (advertised as `kafka:9092` inside the Compose network)
- PostgreSQL: `localhost:5432` (user/password `creditflow` / `creditflow`)

### Automated test script

- **Linux/macOS / Git Bash:** `./scripts/test-flow.sh`
- **Windows PowerShell:** `.\scripts\test-flow.ps1`

Optional env overrides: `BASE_USER`, `BASE_TX`, `BASE_CREDIT` (default to `http://localhost:8081` etc.).

### Verify Kafka (optional)

```bash
docker compose exec kafka /opt/bitnami/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic transaction-events --from-beginning --max-messages 1
```

## Running services locally with sbt (infra already running)

With PostgreSQL and Kafka reachable (e.g. after **Local development (no Docker)** above, or `docker compose up postgres zookeeper kafka kafka-init`):

```bash
sbt "userService/run"
# other terminals:
sbt "transactionService/run"
sbt "creditService/run"
```

Set `JDBC_URL`, `KAFKA_BOOTSTRAP_SERVERS`, `JWT_SECRET`, and ports via env or `application.conf`.

## API reference

### User service (`:8081`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/users/register` | Create user |
| POST | `/users/login` | Returns JWT |
| GET | `/users/me` | Bearer JWT required |

**Register — request**

```json
{ "email": "user@example.com", "password": "secret123" }
```

**Register — response (201)**

```json
{ "id": "uuid", "email": "user@example.com" }
```

**Login — response**

```json
{ "token": "…", "tokenType": "Bearer" }
```

**Me — response**

```json
{ "id": "uuid", "email": "user@example.com" }
```

### Transaction service (`:8082`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/transactions` | Create transaction; emits Kafka event |

**Request**

```json
{ "userId": "uuid-string", "amount": 100.50, "type": "credit" }
```

`type` is `debit` or `credit` (case-insensitive). Amount is stored and published as a positive magnitude.

**Response (201)**

```json
{ "id": "uuid", "userId": "…", "amount": 100.50, "type": "credit" }
```

**Event payload** (topic `transaction-events`):

```json
{
  "userId": "…",
  "amount": 100.5,
  "type": "credit",
  "timestamp": "2026-04-07T12:00:00Z"
}
```

### Credit service (`:8083`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/credit-score/{userId}` | Current score (defaults to **600** if no record) |

**Response**

```json
{ "userId": "…", "score": 607 }
```

## Project layout

```
creditflow/
  build.sbt
  shared/                 # Kafka event schema + Circe JSON helpers
  user-service/
  transaction-service/
  credit-service/
  docker/
    postgres/             # Custom image chmods init shell scripts
  docker-compose.yml
  scripts/
    test-flow.sh
    test-flow.ps1
    local/                  # No-Docker: Postgres + Kafka topic helpers
      setup-postgres.ps1
      setup-postgres.sh
      create-kafka-topic.ps1
      create-kafka-topic.sh
      sql/
```

## Operations notes

- **At-least-once delivery:** The transaction service writes to PostgreSQL then publishes to Kafka; on rare failures you could see a saved transaction without an event (or retry duplicates). Production systems often use the **outbox pattern** or idempotent consumers.
- **JWT secret:** Set `JWT_SECRET` in deployment; the Compose file uses a development placeholder.

## License

Sample / portfolio project — use and modify freely.
