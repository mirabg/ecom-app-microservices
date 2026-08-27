# Keycloak realm provisioning

`realm-export/ecom-app-realm.json` is imported automatically on startup. The
compose service mounts this directory at `/opt/keycloak/data/import` and runs
Keycloak with `start-dev --import-realm`.

Import uses the `IGNORE_EXISTING` strategy, so it is a no-op when the realm is
already present. It only re-provisions after the Postgres volume is destroyed
(`docker compose down -v`), which is exactly when you need it.

## What the file defines

| Item | Why |
|---|---|
| Realm roles `USER`, `PRODUCT`, `ORDER` | The gateway's `SecurityConfig` gates `/api/users/**`, `/api/products/**` and `/api/orders/**` + `/api/cart/**` on these. `USER` is also `keycloak.default-role`, which user-service assigns to every user it creates. |
| Client `user-service-admin` | Confidential client with a service account, used by `KeycloakAdminClient` for Admin REST API calls. Never used for end-user login. `directAccessGrantsEnabled` is on only so tokens can be minted for testing. |
| Service account role mappings | See below. |

## Service account permissions

`service-account-user-service-admin` is granted these `realm-management` roles:

| Role | Needed for |
|---|---|
| `manage-users` | `POST /users` - creating the Keycloak user |
| `view-users` / `query-users` | Looking users up |
| `view-realm` | `GET /roles/{roleName}` in `KeycloakAdminClient.assignRealmRole`. **Without this the role lookup returns 403**, the user is still created but ends up with no realm roles, and every gateway request for that user then fails with 403. |

## Caveats

**Token caching.** `KeycloakAdminClient` caches the service account access
token. If you change these role mappings on a running system, restart
user-service (or wait for the token to expire) before the new permissions take
effect.

**The client secret is committed here** and must match
`keycloak.admin-client-secret` (env `KEYCLOAK_ADMIN_CLIENT_SECRET`) in
user-service. Fine for local development; for anything shared, generate a new
secret and inject it via `.env` instead.

## Re-exporting after console changes

If you change the realm through the admin console and want to keep it, either
edit this file by hand (preferred - it stays small and version-agnostic) or
partial-export and prune:

```bash
TOKEN=$(curl -s -X POST http://localhost:8180/realms/master/protocol/openid-connect/token \
  -d client_id=admin-cli -d username=admin -d password=admin -d grant_type=password \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")

curl -s -X POST "http://localhost:8180/admin/realms/ecom-app/partial-export?exportClients=true&exportGroupsAndRoles=true" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

Note that a partial export masks client secrets as `**********` and omits
service account users entirely, so those two parts always need re-adding by
hand.

