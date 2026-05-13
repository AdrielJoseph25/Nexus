# Nexus Distributed Microservices E-Commerce Backend

Dark-themed Java 17 backend project using Spring Boot, Spring Cloud Gateway, PostgreSQL, and Kafka. The system decouples order, inventory, and payment ownership while coordinating consistency through a choreography-based Saga Pattern.

## Architecture

- `frontend` contains the dark themed browser app for creating orders and viewing saga state.
- `gateway` routes public API traffic and serves the dark control page at `http://localhost:8080`.
- `order-service` owns order state and saga outcomes.
- `inventory-service` reserves and releases stock.
- `payment-service` authorizes payments and emits success or rollback events.
- `common` contains shared DTOs, Kafka event contracts, and topic names.

## Saga Flow

1. `POST /api/orders` creates a pending order and publishes `order.created`.
2. Inventory reserves stock and publishes `inventory.reserved`, or publishes `inventory.rejected`.
3. Payment handles reserved orders and publishes `payment.completed`, or `payment.failed`.
4. Order service confirms completed payments.
5. On payment failure, order service cancels the order and publishes `inventory.release` to roll back stock.

Payments above `5000.00` intentionally fail so the rollback path is easy to test.

## Run Locally

Requirements:

- Java 17
- Maven 3.9+
- Docker Desktop

Start infrastructure:

```bash
docker compose up -d
```

Build:

```bash
mvn clean package
```

Run services in separate terminals:

```bash
mvn -pl order-service spring-boot:run
mvn -pl inventory-service spring-boot:run
mvn -pl payment-service spring-boot:run
mvn -pl gateway spring-boot:run
```

Open the dark control page:

```text
http://localhost:8080
```

Open the frontend app:

```text
frontend/index.html
```

The frontend calls the Gateway API at `http://localhost:8080/api`. To point it somewhere else, set `localStorage.nexusApiBase` in the browser console:

```js
localStorage.setItem("nexusApiBase", "http://localhost:8080/api")
```

Create a successful order:

```bash
curl -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"cust-1001","sku":"NX-LAPTOP-17","quantity":1,"amount":1299.00}'
```

Create a rollback scenario:

```bash
curl -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"cust-1002","sku":"NX-LAPTOP-17","quantity":1,"amount":7500.00}'
```

Inspect state:

```bash
curl http://localhost:8080/api/orders
curl http://localhost:8080/api/inventory
curl http://localhost:8080/api/payments
```

## Ports

| Component | Port |
| --- | --- |
| Gateway | `8080` |
| Order service | `8081` |
| Inventory service | `8082` |
| Payment service | `8083` |
| Kafka | `9092` |
| Orders PostgreSQL | `5432` |
| Inventory PostgreSQL | `5433` |
| Payments PostgreSQL | `5434` |

## Frontend

The dark frontend includes:

- Dashboard cards for total orders, confirmed orders, reserved inventory, and payments.
- Order creation form that starts the Kafka saga.
- Rollback demo button that sets an amount above the payment limit.
- Tables for orders, inventory, and payments.

## Resume Highlights

- Architected a distributed microservices backend that decouples order, inventory, and payment domains.
- Implemented choreography-based Saga rollback through Kafka events.
- Added optimized Spring Cloud Gateway routing and a dark operational landing page.
