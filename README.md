# Store API

RESTful API for a store application: user registration and login, a session-scoped shopping cart,
checkout against live stock, and order history. Spring Boot 4.1 on Java 21+, persisted in an in-memory
H2 database that is seeded with a product catalogue on startup.

## Running

```bash
./mvnw spring-boot:run        # starts on http://localhost:8080
./mvnw test                   # 92 tests + JaCoCo report in target/site/jacoco
```

## Authentication

`POST /api/users/login` returns a `sessionId`. Every cart, checkout and order call must send it as a
header:

```
X-Session-Id: <sessionId>
```

A session lives for 30 minutes. Each login opens a new session, and every session starts with its own
empty cart, so an unknown or expired session identifier is rejected with `401` and the next login
begins with a fresh cart.

## Endpoints

| Method | Path | Purpose | Success | Failure |
|---|---|---|---|---|
| POST | `/api/users/register` | Register `{"email","password"}` | `200` | `409` email taken, `400` invalid |
| POST | `/api/users/login` | Open a session | `200` | `401` bad credentials, `423` locked |
| POST | `/api/users/password-reset/request` | Issue a reset token for an email | `200` | `404` unknown email |
| POST | `/api/users/password-reset` | Apply `{"token","newPassword"}` | `204` | `400` token invalid/used/expired |
| GET | `/api/products` | List the catalogue | `200` | |
| POST | `/api/cart` | Add one position `{"id","quantity"}` | `201` | `409` insufficient stock, `404` unknown product |
| GET | `/api/cart` | View positions with ordinals and subtotals | `200` | `401` no/expired session |
| PATCH | `/api/cart/items/{itemId}` | Change quantity `{"quantity"}` | `200` | `409` insufficient stock, `404` unknown item |
| DELETE | `/api/cart/items/{itemId}` | Remove a position | `204` | `404` unknown item |
| POST | `/api/checkout` | Buy the cart, deduct stock | `201` | `400` empty cart, `409` stock or price changed |
| GET | `/api/orders` | Order history | `200` | `401` no/expired session |
| POST | `/api/orders/{orderId}/cancel` | Cancel and restock | `200` | `409` already cancelled, `404` not yours |

Errors share one shape: `{"status":409,"error":"Conflict","message":"..."}`.

### Example

```bash
curl -X POST localhost:8080/api/users/register -H 'Content-Type: application/json' \
     -d '{"email":"my@email.com","password":"123"}'

SID=$(curl -s -X POST localhost:8080/api/users/login -H 'Content-Type: application/json' \
     -d '{"email":"my@email.com","password":"123"}' | jq -r .sessionId)

curl -X POST localhost:8080/api/cart -H "X-Session-Id: $SID" \
     -H 'Content-Type: application/json' -d '{"id":"2411","quantity":"2"}'

curl localhost:8080/api/cart -H "X-Session-Id: $SID"
curl -X POST localhost:8080/api/checkout -H "X-Session-Id: $SID"
```

## Design notes

- **Package per domain** (`user`, `session`, `catalog`, `cart`, `order`, `common`). Each service is an
  interface with a `Default…` implementation, so callers depend on abstractions.
- **Stock** is validated when a position is added or changed, re-validated at checkout, deducted only
  on a successful checkout, and returned when an order is cancelled. `Product` owns those rules
  (`ensureAvailable`, `reduceAvailable`, `restock`).
- **Prices** are `BigDecimal` at scale 2 (`Money`). A cart position stores the price the customer saw;
  checkout rejects the purchase with `409` if the catalogue price has changed since.
- **Passwords** are stored as BCrypt hashes, never in plain text.
- **Brute force** protection: 5 failed logins lock the account for 15 minutes (`423`), and a successful
  login clears the counter. The counter is committed by `DefaultLoginAttemptPolicy` in its own
  transaction, so it survives the rejected login — `LoginLockoutIntegrationTest` guards this.
- **Time** comes from an injected `Clock`, which keeps expiry and lockout logic testable.
- Configuration lives under the `store.*` keys in `application.yml` (session TTL, lockout policy,
  reset-token TTL).

## Tests

92 tests: isolated Mockito unit tests per service, `@WebMvcTest` slices per controller for status
codes and payloads, and `@SpringBootTest` journeys over the real H2 database. JaCoCo reports ~98%
instruction and ~93% branch coverage of `src/main/java`.
