# ecom-app

A Spring Boot / Spring Cloud microservices e-commerce demo: API gateway, service
discovery, centralized config, JWT auth via Keycloak, async messaging over
RabbitMQ, and distributed tracing to Zipkin. Everything runs in Docker.

## Services

| Service | Port | Purpose | Stores data in |
|---|---|---|---|
| `eureka` | 8761 | Service discovery | - |
| `config-server` | 8888 | Serves centralized config from `config-server/src/main/resources/config/` | - |
| `api-gateway` | 8080 | Single entry point, JWT validation, routing, rate limiting | - |
| `user-service` | 8082 | Users; provisions matching accounts in Keycloak | MongoDB |
| `product-service` | 8083 | Product catalogue | Postgres `productdb` |
| `order-service` | 8084 | Carts and orders; calls product + user via Eureka | Postgres `ordersdb` |
| `notification-service` | 8085 | Consumes order events | - |

### Infrastructure

| Component | Port | Notes |
|---|---|---|
| Postgres | 5432 | Databases created by `init-db.sql` |
| MongoDB | 27017 | |
| RabbitMQ | 5672 / 15672 | Management UI at :15672 (guest/guest) |
| Keycloak | 8180 | Admin console at :8180 (admin/admin) |
| Zipkin | 9411 | |
| pgAdmin | 5050 | |
| Kafka | 9092 | Present but not currently used by any service |

## Prerequisites

- Docker Desktop (or any Docker engine with Compose v2)
- JDK 25+ (the config-server module targets 26) — only needed to build the jars
- ~6 GB free RAM for the full stack

## Quick start

The Dockerfiles copy a pre-built fat jar from each module's `target/`, so the
jars must be built **before** the images.

```bash
# 1. build every jar
for m in eureka config-server api-gateway user product orders notification-service; do
  (cd "$m" && ./mvnw clean package -DskipTests) || break
done

# 2. build images and start everything
docker compose up -d --build

# 3. watch it come up (config clients retry for ~2 min while config-server boots)
docker compose ps
```

First start takes 1-2 minutes: Keycloak imports its realm, Postgres runs
`init-db.sql`, and the Spring services retry against `config-server` until it
is serving.

No `.env` file is required — `docker-compose.yml` provides defaults for every
variable. Copy `.env.example` to `.env` only if you want to override something.

### Verify

```bash
# health of each service (gateway returns 401 by design - its actuator is secured)
for p in 8761 8082 8083 8084 8085; do
  printf "%s -> " "$p"; curl -s "http://localhost:$p/actuator/health" | head -c 40; echo
done

# six services registered, each with a container hostname
open http://localhost:8761
```

### Try it

```bash
# 1. sign up (goes directly to user-service: the gateway requires a token,
#    which a brand-new user does not have yet)
curl -X POST http://localhost:8082/api/users -H "Content-Type: application/json" -d '{
  "firstName":"Ada","lastName":"Lovelace","email":"ada@example.com",
  "phone":"555-0102",
  "address":{"street":"1 Test Way","city":"Austin","state":"TX","zip":"78701","country":"US"},
  "password":"Passw0rd!"
}'
# -> note the returned "id" (Mongo id, used as X-User-ID below)

# 2. log in
TOKEN=$(curl -s -X POST http://localhost:8180/realms/ecom-app/protocol/openid-connect/token \
  -d client_id=user-service-admin \
  -d client_secret=2iaX0dpf1zkXq7k7fLKHj9Yjg2rOs41L \
  -d username=ada@example.com -d password='Passw0rd!' -d grant_type=password \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")

# 3. add a product (needs the PRODUCT staff role, so go direct for now)
curl -X POST http://localhost:8083/api/products -H "Content-Type: application/json" -d '{
  "name":"Demo Phone","description":"6.5 inch OLED","price":699.99,
  "stockQuantity":25,"category":"Electronics","imageUrl":"https://example.com/p.jpg"}'

# 4. browse + shop through the gateway
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/products
curl -X POST http://localhost:8080/api/cart \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -H "X-User-ID: <id from step 1>" -d '{"productId":1,"quantity":2}'
```

There are also ready-made request collections: `product-endpoints.http`,
`orders-endpoints.http`, `user-endpoints.http`.

## Authorization model

Keycloak realm roles, enforced by `api-gateway`'s `SecurityConfig`:

| Route | Required role |
|---|---|
| `/api/users/**` | `USER` |
| `GET /api/products/**` | `USER` or `PRODUCT` |
| `POST/PUT/DELETE /api/products/**` | `PRODUCT` |
| `/api/cart/**`, `/api/orders/**` | `USER` or `ORDER` |

`USER` is assigned automatically to every account user-service creates
(`keycloak.default-role`). `PRODUCT` and `ORDER` are back-office roles that
must be granted manually in the Keycloak console.

The realm, its roles, the `user-service-admin` client and its service-account
permissions are imported automatically — see [keycloak/README.md](keycloak/README.md).

## Running a service from your IDE

Every host in the configuration is an overridable placeholder whose **default is
`localhost`**, so a service started from the IDE works against the Dockerised
infrastructure with no changes. Start the dependencies only:

```bash
docker compose up -d postgres mongodb rabbitmq keycloak zipkin eureka config-server
```

then run the module. To run a service on the host while the *rest* is
containerized, stop its container first (`docker compose stop order-service`) so
the two do not both register in Eureka under the same name.

## Configuration

Config lives in two places:

1. **Bootstrap** — each module's `src/main/resources/application.yaml`. Only the
   app name and how to reach the config server.
2. **Everything else** — `config-server/src/main/resources/config/<service>.yaml`,
   served over HTTP. See [config-server/README.md](config-server/README.md).

Changing a served config file requires rebuilding the config-server jar and
image, because the files are packaged inside it:

```bash
(cd config-server && ./mvnw clean package -DskipTests)
docker compose up -d --build config-server
docker compose restart product-service order-service user-service notification-service api-gateway
```

### Environment variables

Set in `docker-compose.yml` per service; `.env` can override the credential
ones. See `.env.example` for the full list with explanations.

| Variable | Used by | Default |
|---|---|---|
| `CONFIG_SERVER_URL` | all config clients | `http://localhost:8888` |
| `EUREKA_URL` | all Eureka clients | `http://localhost:8761/eureka/` |
| `EUREKA_INSTANCE_HOSTNAME` | all Eureka clients | `localhost` |
| `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD` | product, order, postgres, keycloak | `localhost`, `5432`, `root`, `root` |
| `MONGO_URL` | user-service | `mongodb://localhost:27017/userdb` |
| `RABBITMQ_HOST`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD` | all bus clients + broker | `localhost`, `guest`, `guest` |
| `ZIPKIN_TRACING_ENDPOINT` | gateway, product, order, user | `http://localhost:9411/api/v2/spans` |
| `KEYCLOAK_ISSUER_URI` / `KEYCLOAK_JWK_SET_URI` | api-gateway | `http://localhost:8180/...` |
| `KEYCLOAK_SERVER_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_ADMIN_CLIENT_*` | user-service | see `.env.example` |
| `JPA_DDL_AUTO` | product, order | `update` |

## Common operations

```bash
docker compose logs -f order-service          # tail one service
docker compose up -d --build product-service  # rebuild one service (after ./mvnw package)
docker compose restart user-service           # restart
docker compose down                           # stop, keep data
docker compose down -v                        # stop and DELETE all data
```

Application logs are also written to `./logs/<service>.log` on the host via a
bind mount.

## Troubleshooting

**A service restarts repeatedly at startup.** It could not reach
`config-server`. Config clients use `fail-fast: true` with ~2 minutes of
retries and then exit, and `restart: unless-stopped` brings them back. Check
`docker compose logs config-server` first.

**Gateway returns 401 for everything, including `/actuator/health`.** Expected:
`SecurityConfig` protects every exchange. Pass a bearer token.

**Gateway returns 403 with a valid token.** The account lacks the required realm
role — see the authorization table above. Note that `KeycloakAdminClient`
caches its admin token, so after changing role mappings you must restart
user-service.

**Gateway returns 503 "temporarily unavailable" right after restarting a
service.** Expected for up to ~60s. A recreated container gets a new IP, and
the gateway only refetches the Eureka registry every 30s, so the first calls hit
a stale instance and trip the circuit-breaker fallback. It recovers on its own —
verify with `curl http://localhost:8080/actuator/circuitbreakers` (needs a
token) and just retry.

**`lb://SERVICE` fails to resolve.** The target must be registered in Eureka
with a hostname other containers can resolve. Check http://localhost:8761 —
`prefer-ip-address` is deliberately `false` so containers advertise their
compose service name rather than an unreachable bridge IP.

**Data disappeared after a restart.** `JPA_DDL_AUTO` is set to `create`
somewhere; it should be `update`.

**Realm `ecom-app` does not exist.** Keycloak only imports on an empty
database. Run `docker compose down -v` and start again.

### Spring Boot 4 property renames

These were all silently ignored (deprecated at ERROR level) and fell back to
`localhost` defaults, which looked fine until the apps moved into containers.
Watch for them if you add services:

| Old | New |
|---|---|
| `spring.data.mongodb.uri` | `spring.mongodb.uri` |
| `management.zipkin.tracing.endpoint` | `management.tracing.export.zipkin.endpoint` |
| `spring-boot-starter-aop` (artifact) | `spring-boot-starter-aspectj` |

## Known gaps

- **Sign-up bypasses the gateway.** `POST /api/users` requires the `USER` role,
  which a new user cannot have. Onboarding currently goes directly to
  user-service on :8082; a `permitAll()` rule for that one route is needed
  before this is exposed anywhere real.
- **Secrets are committed.** The Keycloak client secret appears in
  `docker-compose.yml`, `.env.example` and the realm import file. Fine for a
  local demo, not for anything shared.
- **Kafka is running but unused.** No service has a Kafka dependency.

