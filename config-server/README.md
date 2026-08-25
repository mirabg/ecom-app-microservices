# Config Server Local Configuration

This config server now serves service configuration from local resource files instead of the external Git repository.

## Local config files

These files live under `src/main/resources/config`:

- `order-service.yaml`
- `product-service.yaml`
- `user-service.yaml`

## Client services

Services continue to import configuration from the same config server URL:

- `http://localhost:8888`

So no client-side config location change is required as long as the config server remains available on port `8888`.

## Starting config-server locally

Start the config server with the `native` profile enabled so it serves the local files from `src/main/resources`:

```zsh
cd /Users/greg/tutorials/ecom-app/config-server
SPRING_PROFILES_ACTIVE=native ./mvnw spring-boot:run
```

## After changing service config

Restart `config-server`, then restart any consuming services (`orders`, `product`, `user`) so they reload the latest configuration.

