package com.ecom.user.client;

import com.ecom.user.config.KeycloakProperties;
import com.ecom.user.dto.UserRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around the Keycloak Admin REST API used to provision an
 * end-user account whenever a local {@link com.ecom.user.model.User} is
 * created, so the two records share the same identity (User.keycloakId ==
 * the "sub" claim later found in that user's JWTs).
 */
@Component
public class KeycloakAdminClient {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminClient.class);

    private final RestClient restClient;
    private final KeycloakProperties properties;

    private volatile String cachedAdminToken;
    private volatile Instant cachedAdminTokenExpiry = Instant.EPOCH;

    public KeycloakAdminClient(RestClient.Builder restClientBuilder, KeycloakProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    /**
     * Builds the realm base path, e.g. http://localhost:8180/admin/realms/ecom-app.
     * NOTE: don't pass serverUrl as a RestClient URI *template variable* - values
     * substituted into a template are percent-encoded (so "http://host" becomes
     * "http%3A%2F%2Fhost" and gets treated as a single path segment instead of a
     * scheme+host). Build the literal base URL here and only template the
     * remaining path segments.
     */
    private String adminRealmBaseUrl() {
        return properties.getServerUrl() + "/admin/realms/" + properties.getRealm();
    }

    private String realmBaseUrl() {
        return properties.getServerUrl() + "/realms/" + properties.getRealm();
    }

    /**
     * Creates the user in Keycloak (with the supplied password as their
     * initial, non-temporary credential) and returns the Keycloak-assigned
     * user id.
     */
    public String createUser(UserRequest request) {
        Map<String, Object> body = Map.of(
                "username", request.getEmail(),
                "email", request.getEmail(),
                "firstName", request.getFirstName(),
                "lastName", request.getLastName(),
                "enabled", true,
                "emailVerified", false,
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", request.getPassword(),
                        "temporary", false
                ))
        );

        URI location = restClient.post()
                .uri(adminRealmBaseUrl() + "/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + fetchAdminAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity()
                .getHeaders()
                .getLocation();

        if (location == null) {
            throw new IllegalStateException("Keycloak did not return a Location header for the created user");
        }

        // Location: {serverUrl}/admin/realms/{realm}/users/{id}
        String path = location.getPath();
        String keycloakId = path.substring(path.lastIndexOf('/') + 1);

        try {
            assignRealmRole(keycloakId, properties.getDefaultRole());
        } catch (RuntimeException ex) {
            // Role assignment failing shouldn't block user creation, but it must
            // not leave a silently mis-provisioned account either - log loudly.
            log.warn("Created Keycloak user {} but failed to assign default role '{}'",
                    keycloakId, properties.getDefaultRole(), ex);
        }

        return keycloakId;
    }

    /** Compensating action used if the local DB write fails after Keycloak user creation succeeds. */
    public void deleteUser(String keycloakId) {
        try {
            restClient.delete()
                    .uri(adminRealmBaseUrl() + "/users/{id}", keycloakId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + fetchAdminAccessToken())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException ex) {
            log.error("Failed to roll back Keycloak user {} after a local persistence failure - " +
                    "an orphaned Keycloak account may now exist and require manual cleanup", keycloakId, ex);
        }
    }

    private void assignRealmRole(String keycloakId, String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return;
        }

        Map<String, Object> role = restClient.get()
                .uri(adminRealmBaseUrl() + "/roles/{roleName}", roleName)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + fetchAdminAccessToken())
                .retrieve()
                .body(new ParameterizedTypeReference<>() { });

        if (role == null) {
            log.warn("Realm role '{}' not found in Keycloak realm '{}' - skipping role assignment",
                    roleName, properties.getRealm());
            return;
        }

        restClient.post()
                .uri(adminRealmBaseUrl() + "/users/{id}/role-mappings/realm", keycloakId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + fetchAdminAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve()
                .toBodilessEntity();
    }

    private synchronized String fetchAdminAccessToken() {
        if (cachedAdminToken != null && Instant.now().isBefore(cachedAdminTokenExpiry)) {
            return cachedAdminToken;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.getAdminClientId());
        form.add("client_secret", properties.getAdminClientSecret());

        TokenResponse response = restClient.post()
                .uri(realmBaseUrl() + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException("Failed to obtain a Keycloak admin access token - " +
                    "check keycloak.admin-client-id/admin-client-secret and that the client's " +
                    "service account has the realm-management 'manage-users' role");
        }

        cachedAdminToken = response.accessToken();
        // Refresh a little early so we never use a token expiring mid-request.
        cachedAdminTokenExpiry = Instant.now().plusSeconds(Math.max(response.expiresIn() - 10, 0));
        return cachedAdminToken;
    }

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") int expiresIn
    ) {
    }
}

