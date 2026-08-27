# config-server

Serves centralized configuration to every other Spring service. Runs with the
`native` profile, so it reads YAML from its own classpath rather than a Git
repository.

## Served configuration

Files live in `src/main/resources/config/` and are matched by
`spring.application.name`:

| File | Consumed by |
|---|---|
| `api-gateway.yaml` | api-gateway |
| `product-service.yaml` | product-service |
| `order-service.yaml` | order-service |
| `user-service.yaml` | user-service |
| `notification-service.yaml` | notification-service |

`eureka` is deliberately absent - it has no config client and is fully
configured by its own `application.yaml`, since it must be reachable before
anything else starts.

Inspect what is being served at any time:

```bash
curl http://localhost:8888/product-service/default | python3 -m json.tool
```

## How clients find it

Each client's `src/main/resources/application.yaml` contains:

```yaml
spring:
  config:
    import: configserver:${CONFIG_SERVER_URL:http://localhost:8888}
  cloud:
    config:
      fail-fast: true
      retry:
        max-attempts: 20
```

Two things to note:

- There is **no `optional:` prefix**. With it, an unreachable config server is
  silently ignored and the app boots with no datasource, no Eureka URL and no
  security config - a failure that looks like a working app until the first
  request. Without it, startup fails loudly instead.
- `fail-fast` + retry means a client tolerates a slow-starting config-server
  (roughly 2 minutes of retries) but exits if it never appears. This requires
  `spring-retry` and `spring-boot-starter-aspectj` on the client classpath.

In Docker, `CONFIG_SERVER_URL` is set to `http://config-server:8888`. The
`localhost` default keeps IDE runs working unchanged.

## Placeholders

The served files do not hard-code hostnames. Values such as `${DB_HOST:localhost}`
and `${EUREKA_URL:http://localhost:8761/eureka}` are resolved **by the client**,
from its own environment - the config server hands over the raw `${...}` string.
That is why the same file works both in a container and from the IDE.

See the environment variable table in the root [README](../README.md).

## Applying a change

The YAML is packaged inside the jar, so editing a file is not enough. The
Dockerfile compiles the module inside the image, so one command rebuilds and
redeploys:

```bash
docker compose up -d --build config-server

# clients only read config at startup
docker compose restart api-gateway product-service order-service \
                       user-service notification-service
```

Spring Cloud Bus (`spring-cloud-starter-bus-amqp`) is on the classpath, so
`POST /actuator/busrefresh` can broadcast a refresh instead of restarting -
but note that only `@RefreshScope` beans pick up changes that way.

## Actuator is on port 8889

The config server maps `/{application}/{profile}` at the root, which swallows
`/actuator/health` — it resolves as application=`actuator`, profile=`health`
and returns config JSON instead of a health response. Actuator therefore runs
on its own port:

```bash
curl http://localhost:8889/actuator/health
curl http://localhost:8889/actuator/health/readiness   # for Kubernetes probes
```

Note this module needs an explicit `spring-boot-starter-actuator` dependency;
without it there is no `/actuator` at all and `management.server.port` is
silently ignored.

## Running locally

```bash
cd config-server
./mvnw spring-boot:run
```

The `native` profile is already active via `application.yaml`; no extra flags
are needed.
